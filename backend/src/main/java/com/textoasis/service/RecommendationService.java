package com.textoasis.service;

import com.textoasis.dto.RecommendationDto;
import com.textoasis.dto.RecommendationRequestDto;
import com.textoasis.model.*;
import com.textoasis.repository.CityRepository;
import com.textoasis.repository.TagRepository;
import com.textoasis.util.HaversineDistanceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CityRepository cityRepository;
    private final WeatherService weatherService;
    private final TagRepository tagRepository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // --- 评分权重配置 ---
    private static final double W_TAGS = 0.6;     // 标签权重
    private static final double W_DISTANCE = 0.2; // 距离权重
    private static final double W_WEATHER = 0.2;  // 天气权重

    private static final int MAX_TAG_SCORE = 100;
    private static final int MAX_DISTANCE_SCORE = 30;
    private static final int DISTANCE_DECAY_RATE = 10; // 每10公里扣1分
    private static final int MAX_WEATHER_SCORE = 20;

    @Transactional(readOnly = true)
    public List<RecommendationDto> recommend(RecommendationRequestDto request, Optional<User> userOpt) {
        // 0. 解析日期
        Date travelDate;
        try {
            travelDate = (request.getTravelDate() != null && !request.getTravelDate().isEmpty())
                    ? dateFormat.parse(request.getTravelDate())
                    : new Date(); // 如果不提供日期，则默认为今天
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format for travelDate. Please use YYYY-MM-DD.");
        }

        // 1. 获取出发地城市信息
        City departureCity = cityRepository.findByName(request.getDepartureCity())
                .orElseThrow(() -> new IllegalArgumentException("Departure city not found: " + request.getDepartureCity()));

        // 2. 确定用于推荐的兴趣标签和权重
        final Map<String, Double> finalUserInterestMap;
        // 优先使用请求中明确指定的兴趣标签（非空列表才覆盖）
        if (request.getInterestTags() != null && !request.getInterestTags().isEmpty()) {
            finalUserInterestMap = new HashMap<>();
            for (String tagName : request.getInterestTags()) {
                finalUserInterestMap.put(tagName, 1.0); // 显式请求的标签权重为1.0
            }
        } else if (userOpt.isPresent()) {
            // 如果请求中没有指定标签，且用户已登录，则使用用户画像中的兴趣标签
            User user = userOpt.get();
            if (user.getInterestTags() != null && !user.getInterestTags().isEmpty()) {
                finalUserInterestMap = user.getInterestTags().stream()
                        .collect(Collectors.toMap(
                                userInterestTag -> userInterestTag.getTag().getName(),
                                UserInterestTag::getWeight
                        ));
            } else {
                finalUserInterestMap = Collections.emptyMap();
            }
        } else {
            finalUserInterestMap = Collections.emptyMap();
        }

        // 3. 获取所有标签并创建索引映射，用于向量化
        List<String> allTagNames = tagRepository.findAll().stream().map(Tag::getName).toList();
        Map<String, Integer> tagIndexMap = new HashMap<>();
        for (int i = 0; i < allTagNames.size(); i++) {
            tagIndexMap.put(allTagNames.get(i), i);
        }

        // 4. 预筛选，缩小候选城市范围
        List<City> candidateCities = getCandidateCities(departureCity, request.getDistanceScope());

        // 5. 对候选城市计算得分
        final Date finalTravelDate = travelDate; // 确保在lambda中可用
        List<RecommendationDto> recommendations = candidateCities.stream()
                .map(city -> {
                    // 6. 为每个城市计算各项原始得分
                    double tagScore = calculateCosineSimilarityScore(city, finalUserInterestMap, tagIndexMap);
                    double distance = HaversineDistanceUtil.calculate(
                            departureCity.getLatitude().doubleValue(), departureCity.getLongitude().doubleValue(),
                            city.getLatitude().doubleValue(), city.getLongitude().doubleValue()
                    );
                    double distanceScore = calculateDistanceScore(distance);

                    Optional<WeatherForecast> forecastOpt = weatherService.getWeatherForecast(city, finalTravelDate);
                    double weatherScore = calculateWeatherScore(forecastOpt);

                    // 7. 归一化并加权计算总分
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
                    dto.setWeatherForecast(forecastOpt.map(WeatherForecast::getText).orElse("未知"));
                    dto.setMatchedTags(getMatchedTags(city, finalUserInterestMap.keySet()));
                    dto.setLatitude(city.getLatitude());
                    dto.setLongitude(city.getLongitude());
                    return dto;
                })
                .collect(Collectors.toList());

        // 8. 按总分降序排序并返回Top 10
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

    private double calculateCosineSimilarityScore(City city, Map<String, Double> userInterestMap, Map<String, Integer> tagIndexMap) {
        if (userInterestMap == null || userInterestMap.isEmpty()) {
            return 0;
        }

        int vectorSize = tagIndexMap.size();
        double[] userVector = new double[vectorSize];
        double[] cityVector = new double[vectorSize];

        // 构建用户兴趣向量 (使用权重)
        for (Map.Entry<String, Double> entry : userInterestMap.entrySet()) {
            Integer index = tagIndexMap.get(entry.getKey());
            if (index != null) {
                userVector[index] = entry.getValue();
            }
        }

        // 构建城市特征向量 (值为该标签下的景点数量)
        for (CityFeature feature : city.getFeatures()) {
            Integer index = tagIndexMap.get(feature.getTag().getName());
            if (index != null) {
                // 如果一个城市有多个同类型景点，可以累加或设为1，这里简单设为1表示拥有该特征
                cityVector[index] = 1.0;
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

    /**
     * 和风天气官方 textDay 枚举值 -> 基础评分映射。
     * 评分 1-20，越高越适合出游。
     * 精确匹配，不再使用 contains 模糊匹配。
     */
    private static final Map<String, Integer> WEATHER_BASE_SCORES = buildWeatherScores();

    private static Map<String, Integer> buildWeatherScores() {
        Map<String, Integer> m = new LinkedHashMap<>();
        // 未知 / 其他（兜底）
        // 保持 key 为 null 占位，在查找时用 getOrDefault

        // 严重恶劣（1-3分）
        int SEVERE = 2;
        for (String s : new String[]{"特大暴雨", "大暴雨到特大暴雨", "大暴雨", "暴雨到大暴雨", "暴雨",
                "极端降雨", "强沙尘暴", "强雷阵雨伴有冰雹", "雷阵雨伴有冰雹",
                "强雷阵雨", "特强浓雾", "严重霾",
                "暴雪", "大到暴雪"}) m.put(s, SEVERE);

        // 恶劣天气（4-6分）
        int BAD = 5;
        for (String s : new String[]{"大到暴雨", "中到大雨", "大到暴雪",
                "大雨", "大雪",
                "浓雾", "重度霾", "强浓雾", "大雾",
                "沙尘暴", "浮尘", "扬沙"}) m.put(s, BAD);

        // 一般降水 / 轻度影响（7-10分）
        int MODERATE = 8;
        for (String s : new String[]{"中到大雪", "中雨", "中雪",
                "小到中雨", "小到中雪",
                "雨夹雪",
                "雷阵雨", "冻雨",
                "强阵雨",
                "中度霾", "霾",
                "雨雪天气", "雪"}) m.put(s, MODERATE);

        // 轻微影响（11-14分）
        int LIGHT = 12;
        for (String s : new String[]{"小雨", "小雪", "毛毛雨/细雨",
                "阵雨", "阵雪", "阵雨夹雪",
                "薄雾", "雾",
                "冷"}) m.put(s, LIGHT);

        // 阴天（15-16分）
        m.put("阴", 16);

        // 云量较多（17-18分）
        for (String s : new String[]{"多云", "少云", "晴间多云"}) m.put(s, 18);

        // 晴好（19-20分）
        m.put("晴", 20);

        // 热 / 雨（宽泛值）
        m.put("热", 14);
        m.put("雨", 6);

        return Collections.unmodifiableMap(m);
    }

    private double calculateWeatherScore(Optional<WeatherForecast> forecastOpt) {
        if (forecastOpt.isEmpty()) {
            return MAX_WEATHER_SCORE * 0.5; // 如果没有天气数据，返回一个中性分数
        }
        WeatherForecast forecast = forecastOpt.get();

        // 1. 基础分：精确匹配和风官方枚举值
        String weatherText = forecast.getText();
        double baseScore = WEATHER_BASE_SCORES.getOrDefault(weatherText, 10);

        // 2. 温度惩罚系数
        double tempFactor = 1.0;
        try {
            int avgTemp = (Integer.parseInt(forecast.getTempMax()) + Integer.parseInt(forecast.getTempMin())) / 2;
            if (avgTemp > 32 || avgTemp < 0) {
                tempFactor = 0.8; // 过热或过冷
            }
        } catch (NumberFormatException e) {
            // 忽略温度解析错误
        }
        
        // 3. 降水惩罚系数
        double precipFactor = 1.0;
        try {
            double precip = Double.parseDouble(forecast.getPrecipitation());
            if (precip > 5.0) {
                precipFactor = 0.7; // 降水较多
            } else if (precip > 0.1) {
                precipFactor = 0.9;
            }
        } catch (NumberFormatException | NullPointerException e) {
            // 忽略降水解析错误
        }

        // 4. 能见度加成/惩罚系数
        double visFactor = 1.0;
        try {
            int visibility = Integer.parseInt(forecast.getVisibility());
            if (visibility < 5) {
                visFactor = 0.8; // 能见度差
            } else if (visibility > 15) {
                visFactor = 1.1; // 能见度极佳
            }
        } catch (NumberFormatException | NullPointerException e) {
            // 忽略能见度解析错误
        }

        return Math.min(MAX_WEATHER_SCORE, baseScore * tempFactor * precipFactor * visFactor);
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
