package com.textoasis.service;

import com.textoasis.dto.RecommendationDto;
import com.textoasis.dto.RecommendationRequestDto;
import com.textoasis.model.City;
import com.textoasis.model.CityFeature;
import com.textoasis.model.Tag;
import com.textoasis.repository.CityRepository;
import com.textoasis.repository.TagRepository;
import com.textoasis.util.HaversineDistanceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CityRepository cityRepository;
    private final WeatherService weatherService;
    private final TagRepository tagRepository;

    // --- 评分权重配置 ---
    private static final double W_TAGS = 0.6;     // 标签权重
    private static final double W_DISTANCE = 0.2; // 距离权重
    private static final double W_WEATHER = 0.2;  // 天气权重

    private static final int MAX_TAG_SCORE = 100;
    private static final int MAX_DISTANCE_SCORE = 30;
    private static final int DISTANCE_DECAY_RATE = 10; // 每10公里扣1分
    private static final int MAX_WEATHER_SCORE = 20;

    public List<RecommendationDto> recommend(RecommendationRequestDto request) {
        // 1. 获取出发地城市信息
        City departureCity = cityRepository.findByName(request.getDepartureCity())
                .orElseThrow(() -> new IllegalArgumentException("Departure city not found: " + request.getDepartureCity()));

        // 2. 获取所有标签并创建索引映射，用于向量化
        List<String> allTagNames = tagRepository.findAll().stream().map(Tag::getName).toList();
        Map<String, Integer> tagIndexMap = new HashMap<>();
        for (int i = 0; i < allTagNames.size(); i++) {
            tagIndexMap.put(allTagNames.get(i), i);
        }

        // 3. 预筛选，缩小候选城市范围
        List<City> candidateCities = getCandidateCities(departureCity, request.getDistanceScope());

        // 4. 对候选城市计算得分
        List<RecommendationDto> recommendations = candidateCities.stream()
                .map(city -> {
                    // 5. 为每个城市计算各项原始得分
                    double tagScore = calculateCosineSimilarityScore(city, new HashSet<>(request.getInterestTags()), tagIndexMap);
                    double distance = HaversineDistanceUtil.calculate(
                            departureCity.getLatitude().doubleValue(), departureCity.getLongitude().doubleValue(),
                            city.getLatitude().doubleValue(), city.getLongitude().doubleValue()
                    );
                    double distanceScore = calculateDistanceScore(distance);
                    String weather = weatherService.getWeather(city, request.getTravelDate());
                    double weatherScore = calculateWeatherScore(weather);

                    // 6. 归一化并加权计算总分
                    double normalizedTagScore = (MAX_TAG_SCORE > 0) ? (tagScore / MAX_TAG_SCORE) : 0;
                    double normalizedDistanceScore = (MAX_DISTANCE_SCORE > 0) ? (distanceScore / MAX_DISTANCE_SCORE) : 0;
                    double normalizedWeatherScore = (MAX_WEATHER_SCORE > 0) ? (weatherScore / MAX_WEATHER_SCORE) : 0;

                    double totalScore = (normalizedTagScore * W_TAGS +
                                         normalizedDistanceScore * W_DISTANCE +
                                         normalizedWeatherScore * W_WEATHER) * 100; // 转换为百分制

                    RecommendationDto dto = new RecommendationDto();
                    dto.setCityId(city.getId());
                    dto.setName(city.getName());
                    dto.setProvince(city.getProvince());
                    dto.setScore(Math.round(totalScore * 100.0) / 100.0); // 保留两位小数
                    dto.setDistanceKm(Math.round(distance * 100.0) / 100.0);
                    dto.setWeatherForecast(weather);
                    dto.setMatchedTags(getMatchedTags(city, new HashSet<>(request.getInterestTags())));
                    dto.setLatitude(city.getLatitude());
                    dto.setLongitude(city.getLongitude());
                    return dto;
                })
                .collect(Collectors.toList());

        // 7. 按总分降序排序并返回Top 10
        return recommendations.stream()
                .sorted(Comparator.comparingDouble(RecommendationDto::getScore).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<City> getCandidateCities(City departureCity, String distanceScope) {
        // 如果未指定范围，则默认为全国
        if (distanceScope == null || distanceScope.trim().isEmpty()) {
            distanceScope = "ANY";
        }

        List<City> allCities = cityRepository.findAll().stream()
                .filter(city -> !city.getId().equals(departureCity.getId())) // 预先排除出发地
                .collect(Collectors.toList());

        switch (distanceScope) {
            case "PROVINCE":
                return cityRepository.findByProvince(departureCity.getProvince()).stream()
                        .filter(city -> !city.getId().equals(departureCity.getId()))
                        .collect(Collectors.toList());
            case "NEARBY_500KM":
                return allCities.stream()
                        .filter(city -> {
                            double distance = HaversineDistanceUtil.calculate(
                                    departureCity.getLatitude().doubleValue(), departureCity.getLongitude().doubleValue(),
                                    city.getLatitude().doubleValue(), city.getLongitude().doubleValue()
                            );
                            return distance <= 500;
                        })
                        .collect(Collectors.toList());
            case "ANY":
            default:
                return allCities;
        }
    }

    private double calculateCosineSimilarityScore(City city, Set<String> interestTags, Map<String, Integer> tagIndexMap) {
        if (interestTags == null || interestTags.isEmpty()) {
            return 0;
        }

        int vectorSize = tagIndexMap.size();
        double[] userVector = new double[vectorSize];
        double[] cityVector = new double[vectorSize];

        // 构建用户兴趣向量 (选择为1, 未选择为0)
        for (String interestTag : interestTags) {
            Integer index = tagIndexMap.get(interestTag);
            if (index != null) {
                userVector[index] = 1.0;
            }
        }

        // 构建城市特征向量 (值为该标签下的景点数量)
        for (CityFeature feature : city.getFeatures()) {
            Integer index = tagIndexMap.get(feature.getTag().getName());
            if (index != null) {
                cityVector[index] = feature.getAttractions() != null ? feature.getAttractions().size() : 0;
            }
        }

        // 计算余弦相似度
        double dotProduct = 0.0;
        double normUser = 0.0;
        double normCity = 0.0;
        for (int i = 0; i < vectorSize; i++) {
            dotProduct += userVector[i] * cityVector[i];
            normUser += Math.pow(userVector[i], 2);
            normCity += Math.pow(cityVector[i], 2);
        }

        if (normUser == 0 || normCity == 0) {
            return 0.0;
        }

        double similarity = dotProduct / (Math.sqrt(normUser) * Math.sqrt(normCity));
        return similarity * MAX_TAG_SCORE;
    }

    private double calculateDistanceScore(double distanceKm) {
        // 距离分 = max(0, 30 - 距离 / 10)
        double score = MAX_DISTANCE_SCORE - (distanceKm / DISTANCE_DECAY_RATE);
        return Math.max(0, score);
    }

    private double calculateWeatherScore(String weather) {
        double weatherFactor = switch (weather) {
            case "晴" -> 1.0;
            case "多云" -> 0.8;
            case "小雨" -> 0.4;
            case "雪" -> 0.3;
            case "大雨" -> 0.1;
            default -> 0.5; // 其他天气给一个中间值
        };
        return MAX_WEATHER_SCORE * weatherFactor;
    }

    private Set<String> getMatchedTags(City city, Set<String> interestTags) {
        if (interestTags == null || interestTags.isEmpty()) {
            return new HashSet<>();
        }
        return city.getFeatures().stream()
                .map(feature -> feature.getTag().getName())
                .filter(interestTags::contains)
                .collect(Collectors.toSet());
    }
}
