package com.textoasis.service;

import com.textoasis.dto.RecommendationDto;
import com.textoasis.dto.RecommendationRequestDto;
import com.textoasis.model.City;
import com.textoasis.model.CityFeature;
import com.textoasis.repository.CityRepository;
import com.textoasis.util.HaversineDistanceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CityRepository cityRepository;
    private final WeatherService weatherService;

    // --- 评分权重配置 ---
    private static final int TAG_MATCH_SCORE = 50;
    private static final int GOOD_WEATHER_SCORE = 20;
    private static final int BAD_WEATHER_SCORE = -40;
    private static final int DISTANCE_TIER_1_SCORE = 30; // < 200km
    private static final int DISTANCE_TIER_2_SCORE = 15; // < 500km
    private static final int DISTANCE_TIER_3_SCORE = 5;  // < 1000km

    public List<RecommendationDto> recommend(RecommendationRequestDto request) {
        // 1. 获取出发地城市信息
        City departureCity = cityRepository.findByName(request.getDepartureCity())
                .orElseThrow(() -> new IllegalArgumentException("Departure city not found: " + request.getDepartureCity()));

        // 2. 预筛选，缩小候选城市范围
        List<City> candidateCities = getCandidateCities(departureCity, request.getDistanceScope());

        // 3. 对候选城市计算得分
        List<RecommendationDto> recommendations = candidateCities.stream()
                .map(city -> {
                    // 4. 为每个城市计算各项得分
                    double tagScore = calculateTagMatchScore(city, new HashSet<>(request.getInterestTags()));
                    double distance = HaversineDistanceUtil.calculate(
                            departureCity.getLatitude().doubleValue(), departureCity.getLongitude().doubleValue(),
                            city.getLatitude().doubleValue(), city.getLongitude().doubleValue()
                    );
                    double distanceScore = calculateDistanceScore(distance);
                    String weather = weatherService.getWeather(city, request.getTravelDate());
                    double weatherScore = calculateWeatherScore(weather);

                    // 5. 计算总分并构建DTO
                    double totalScore = tagScore + distanceScore + weatherScore;

                    RecommendationDto dto = new RecommendationDto();
                    dto.setCityId(city.getId());
                    dto.setName(city.getName());
                    dto.setProvince(city.getProvince());
                    dto.setScore(totalScore);
                    dto.setDistanceKm(Math.round(distance * 100.0) / 100.0); // 保留两位小数
                    dto.setWeatherForecast(weather);
                    dto.setMatchedTags(getMatchedTags(city, new HashSet<>(request.getInterestTags())));
                    dto.setLatitude(city.getLatitude());
                    dto.setLongitude(city.getLongitude());
                    return dto;
                })
                .collect(Collectors.toList());

        // 6. 按总分降序排序并返回Top 10
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

    private double calculateTagMatchScore(City city, Set<String> interestTags) {
        if (interestTags == null || interestTags.isEmpty()) {
            return 0;
        }
        long matchCount = city.getFeatures().stream()
                .map(feature -> feature.getTag().getName())
                .filter(interestTags::contains)
                .count();
        return matchCount * TAG_MATCH_SCORE;
    }

    private double calculateDistanceScore(double distanceKm) {
        if (distanceKm < 200) return DISTANCE_TIER_1_SCORE;
        if (distanceKm < 500) return DISTANCE_TIER_2_SCORE;
        if (distanceKm < 1000) return DISTANCE_TIER_3_SCORE;
        return 0;
    }

    private double calculateWeatherScore(String weather) {
        if (weather.contains("雨") || weather.contains("雪")) {
            return BAD_WEATHER_SCORE;
        }
        if (weather.contains("晴") || weather.contains("多云")) {
            return GOOD_WEATHER_SCORE;
        }
        return 0;
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
