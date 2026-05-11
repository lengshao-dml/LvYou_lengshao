package com.textoasis.service;

import com.textoasis.dto.PersonalizedRecommendationDto;
import com.textoasis.dto.RecommendationDto;
import com.textoasis.dto.RecommendationRequestDto;
import com.textoasis.model.*;
import com.textoasis.repository.*;
import com.textoasis.util.HaversineDistanceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 个性化推荐服务 —— "猜你喜欢" + 热门标签词云 + 热门城市词云
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizedRecommendationService {

    private final CityRepository cityRepository;
    private final TagRepository tagRepository;
    private final CityPopularityRepository popularityRepository;
    private final UserInterestTagRepository userInterestTagRepository;
    private final UserClickHistoryRepository clickHistoryRepository;
    private final UserSearchHistoryRepository searchHistoryRepository;
    private final UserRecommendLogRepository recommendLogRepository;
    private final RecommendationService recommendationService;

    // 隐式反馈的额外标签权重增量（搜索/点击中出现的非注册兴趣标签）
    private static final double IMPLICIT_TAG_WEIGHT = 0.3;
    // 推荐结果数量
    private static final int PERSONALIZED_COUNT = 6;
    // 词云标签数量
    private static final int HOT_TAG_CLOUD_SIZE = 15;
    // 词云城市数量
    private static final int HOT_CITY_CLOUD_SIZE = 12;

    /**
     * 获取个性化推荐主页数据
     * @param user 登录用户（可能为null表示未登录）
     * @return 包含推荐城市、热门标签词云、热门城市词云的 DTO
     */
    @Transactional(readOnly = true)
    public PersonalizedRecommendationDto getPersonalizedData(Optional<User> userOpt) {
        PersonalizedRecommendationDto result = new PersonalizedRecommendationDto();

        // 提前构建用户兴趣画像（登录用户），供标签词云和推荐共同使用
        Map<String, Double> userProfile = userOpt.isPresent()
                ? buildUserInterestProfile(userOpt.get())
                : Collections.emptyMap();

        // 1. 个性化推荐城市（"猜你喜欢"）
        List<RecommendationDto> personalizedCities;
        if (userOpt.isPresent()) {
            personalizedCities = recommendPersonalizedCities(userOpt.get(), userProfile);
        } else {
            personalizedCities = getHotCityRecommendations();
        }
        result.setRecommendedCities(personalizedCities);

        // 2. 热门标签词云（融合用户兴趣权重）
        result.setHotTags(getHotTagCloud(userProfile));

        // 3. 热门城市词云
        result.setHotCities(getHotCityCloud());

        return result;
    }

    /**
     * 基于用户画像（注册兴趣 + 搜索历史 + 点击历史）进行个性化推荐
     */
    private List<RecommendationDto> recommendPersonalizedCities(User user, Map<String, Double> userProfile) {
        if (userProfile.isEmpty()) {
            log.info("User '{}' has no interest profile, returning hot cities.", user.getUsername());
            return getHotCityRecommendations();
        }

        // 分析用户空间行为：动态出发地 + 距离偏好
        SpatialProfile spatial = analyzeSpatialBehavior(user);

        RecommendationRequestDto request = new RecommendationRequestDto();
        request.setDepartureCity(spatial.effectiveDepartureCity());
        request.setInterestTags(new ArrayList<>(userProfile.keySet()));
        request.setDistanceScope(spatial.effectiveDistanceScope());

        log.info("User '{}' spatial: departure={}, scope={}",
                user.getUsername(), spatial.effectiveDepartureCity(), spatial.effectiveDistanceScope());

        List<RecommendationDto> allScored = recommendationService.recommend(request, Optional.of(user));

        return allScored.stream()
                .sorted(Comparator.comparingDouble(RecommendationDto::getScore).reversed())
                .limit(PERSONALIZED_COUNT)
                .collect(Collectors.toList());
    }

    /**
     * 分析用户空间行为，确定动态出发地和距离偏好。
     * 出发地取最近一次搜索日志；距离偏好基于点击城市的分布。
     */
    private SpatialProfile analyzeSpatialBehavior(User user) {
        // 1. 从最近一次推荐日志获取出发地
        String homeCityName = null;
        City homeCity = null;
        if (user.getHomeCityId() != null) {
            homeCity = cityRepository.findById(user.getHomeCityId()).orElse(null);
            homeCityName = homeCity != null ? homeCity.getName() : null;
        }

        String effectiveDeparture = homeCityName != null ? homeCityName : "北京";
        List<UserRecommendLog> recentLogs = recommendLogRepository
                .findByUserOrderByRequestTimeDesc(user, PageRequest.of(0, 1));
        if (!recentLogs.isEmpty()) {
            String lastDeparture = recentLogs.get(0).getDepartureCity();
            if (lastDeparture != null && !lastDeparture.isBlank()) {
                effectiveDeparture = lastDeparture.trim();
            }
        }

        // 2. 距离偏好：分析点击过的城市距离分布（至少3次点击才触发）
        String effectiveScope = "ANY";
        City departureCity = cityRepository.findByName(effectiveDeparture).orElse(null);
        if (departureCity != null) {
            Set<Long> seenCityIds = new HashSet<>();
            List<UserClickHistory> clicks = clickHistoryRepository.findByUser(user);

            int provinceCount = 0;
            int nearbyCount = 0;
            int totalClicks = 0;

            for (UserClickHistory ch : clicks) {
                if (!seenCityIds.add(ch.getCity().getId())) continue;
                City clickedCity = cityRepository.findById(ch.getCity().getId()).orElse(null);
                if (clickedCity == null) continue;
                totalClicks++;

                if (clickedCity.getProvince() != null
                        && clickedCity.getProvince().equals(departureCity.getProvince())) {
                    provinceCount++;
                }

                double dist = HaversineDistanceUtil.calculate(
                        departureCity.getLatitude().doubleValue(),
                        departureCity.getLongitude().doubleValue(),
                        clickedCity.getLatitude().doubleValue(),
                        clickedCity.getLongitude().doubleValue());
                if (dist <= 500) {
                    nearbyCount++;
                }
            }

            if (totalClicks >= 3) {
                double provinceRatio = (double) provinceCount / totalClicks;
                double nearbyRatio = (double) nearbyCount / totalClicks;

                if (provinceRatio >= 0.6) {
                    effectiveScope = "PROVINCE";
                } else if (nearbyRatio >= 0.6) {
                    effectiveScope = "NEARBY_500KM";
                }
            }
        }

        return new SpatialProfile(effectiveDeparture, effectiveScope);
    }

    /** 空间行为分析结果 */
    private record SpatialProfile(String effectiveDepartureCity, String effectiveDistanceScope) {}

    /**
     * 从用户的历史行为（点击、搜索）中提取兴趣标签，合并到现有用户画像中
     */
    private Map<String, Double> buildUserInterestProfile(User user) {
        // 基础：注册时的兴趣标签权重
        Map<String, Double> profile = new HashMap<>();
        if (user.getInterestTags() != null) {
            for (UserInterestTag uit : user.getInterestTags()) {
                if (uit.getTag() != null) {
                    profile.put(uit.getTag().getName(), uit.getWeight());
                }
            }
        }

        // 从搜索历史中提取兴趣标签
        Set<String> handledSearchKeys = new HashSet<>();
        List<UserSearchHistory> searchHistories = searchHistoryRepository.findByUser(user);
        for (UserSearchHistory sh : searchHistories) {
            String keyword = sh.getKeyword();
            // 去重：同一个关键词只处理一次
            if (!handledSearchKeys.add(keyword)) continue;

            Optional<City> cityOpt = cityRepository.findByName(keyword);
            if (cityOpt.isEmpty() && !keyword.endsWith("市")) {
                cityOpt = cityRepository.findByName(keyword + "市");
            }
            cityOpt.ifPresent(city -> {
                // 重新查询以加载 features（避免懒加载问题）
                City fullCity = cityRepository.findById(city.getId()).orElse(city);
                if (fullCity.getFeatures() != null) {
                    for (CityFeature feature : fullCity.getFeatures()) {
                        if (feature.getTag() != null) {
                            profile.merge(feature.getTag().getName(), IMPLICIT_TAG_WEIGHT, Double::sum);
                        }
                    }
                }
            });
        }

        // 从点击历史中提取兴趣标签
        Set<Long> handledCityIds = new HashSet<>();
        List<UserClickHistory> clickHistories = clickHistoryRepository.findByUser(user);
        for (UserClickHistory ch : clickHistories) {
            if (!handledCityIds.add(ch.getCity().getId())) continue;

            // 重新查询以加载 features
            cityRepository.findById(ch.getCity().getId()).ifPresent(city -> {
                if (city.getFeatures() != null) {
                    for (CityFeature feature : city.getFeatures()) {
                        if (feature.getTag() != null) {
                            profile.merge(feature.getTag().getName(), IMPLICIT_TAG_WEIGHT, Double::sum);
                        }
                    }
                }
            });
        }

        // 归一化权重到 0-1 范围
        if (!profile.isEmpty()) {
            double maxWeight = profile.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            if (maxWeight > 0) {
                for (Map.Entry<String, Double> entry : profile.entrySet()) {
                    profile.put(entry.getKey(), Math.min(1.0, entry.getValue() / maxWeight));
                }
            }
        }

        log.info("Built profile for user '{}': {} tags", user.getUsername(), profile.size());
        return profile;
    }

    /**
     * 未登录用户：返回缓存的热门城市推荐
     */
    private List<RecommendationDto> getHotCityRecommendations() {
        List<CityPopularity> popular = popularityRepository.findByOrderByScoreDesc(
                org.springframework.data.domain.PageRequest.of(0, PERSONALIZED_COUNT));

        return popular.stream().map(p -> {
            RecommendationDto dto = new RecommendationDto();
            City city = p.getCity();
            dto.setCityId(city.getId());
            dto.setName(city.getName());
            dto.setProvince(city.getProvince());
            dto.setScore(p.getScore() != null ? p.getScore() : 0.0);
            dto.setLatitude(city.getLatitude());
            dto.setLongitude(city.getLongitude());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 获取热门标签词云数据，融合全局热度和用户兴趣权重（用户权重 70%，全局热度 30%）
     */
    private List<PersonalizedRecommendationDto.WordCloudItem> getHotTagCloud(Map<String, Double> userProfile) {
        List<Tag> allTags = tagRepository.findAll();
        List<CityPopularity> cityPopularities = popularityRepository.findAll();

        // 1. 计算全局热度分
        Map<String, Long> globalScoreMap = new HashMap<>();
        long maxGlobal = 1;
        for (Tag tag : allTags) {
            if (tag.getFeatures() != null) {
                long totalScore = tag.getFeatures().stream()
                        .filter(f -> f.getCity() != null)
                        .mapToLong(cf -> cityPopularities.stream()
                                .filter(cp -> cp.getCity() != null && cp.getCity().getId().equals(cf.getCity().getId()))
                                .findFirst()
                                .map(cp -> (long) (1 + (cp.getScore() != null ? cp.getScore().longValue() : 0)))
                                .orElse(1L))
                        .sum();
                globalScoreMap.put(tag.getName(), Math.max(1, totalScore));
                if (totalScore > maxGlobal) maxGlobal = totalScore;
            }
        }

        // 2. 融合用户兴趣权重：综合分 = 全局归一化分 * 0.3 + 用户兴趣权重 * 0.7
        Map<String, Double> blendedScores = new HashMap<>();
        for (Tag tag : allTags) {
            double globalNorm = globalScoreMap.containsKey(tag.getName())
                    ? (double) globalScoreMap.get(tag.getName()) / maxGlobal
                    : 0.0;
            double userWeight = userProfile.getOrDefault(tag.getName(), 0.0);
            blendedScores.put(tag.getName(), globalNorm * 0.3 + userWeight * 0.7);
        }

        // 3. 按综合分排序取 top N，转为词云条目
        return blendedScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(HOT_TAG_CLOUD_SIZE)
                .map(entry -> {
                    PersonalizedRecommendationDto.WordCloudItem item = new PersonalizedRecommendationDto.WordCloudItem();
                    item.setName(entry.getKey());
                    item.setCount(globalScoreMap.getOrDefault(entry.getKey(), 1L));
                    item.setWeight(normalizeWeightDouble(entry.getValue(), blendedScores.values()));
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取热门城市词云数据
     */
    private List<PersonalizedRecommendationDto.WordCloudItem> getHotCityCloud() {
        List<CityPopularity> popularities = popularityRepository.findByOrderByScoreDesc(
                org.springframework.data.domain.PageRequest.of(0, HOT_CITY_CLOUD_SIZE));

        if (popularities.isEmpty()) {
            // 如果还没有热门数据，返回所有城市作为兜底
            return cityRepository.findAll().stream()
                    .limit(HOT_CITY_CLOUD_SIZE)
                    .map(city -> {
                        PersonalizedRecommendationDto.WordCloudItem item = new PersonalizedRecommendationDto.WordCloudItem();
                        item.setName(city.getName());
                        item.setCount(1L);
                        item.setWeight(0.5);
                        return item;
                    })
                    .collect(Collectors.toList());
        }

        Collection<Double> allScores = popularities.stream()
                .map(cp -> cp.getScore() != null ? cp.getScore() : 0.0)
                .collect(Collectors.toList());

        return popularities.stream().map(cp -> {
            PersonalizedRecommendationDto.WordCloudItem item = new PersonalizedRecommendationDto.WordCloudItem();
            item.setName(cp.getCity().getName());
            item.setCount(cp.getClickCount() + cp.getSearchCount());
            item.setWeight(normalizeWeightDouble(
                    cp.getScore() != null ? cp.getScore() : 0.0, allScores));
            return item;
        }).collect(Collectors.toList());
    }

    private double normalizeWeight(Long value, Collection<Long> allValues) {
        long max = allValues.stream().mapToLong(Long::longValue).max().orElse(1);
        long min = allValues.stream().mapToLong(Long::longValue).min().orElse(0);
        if (max == min) return 0.5;
        return 0.2 + 0.8 * (double) (value - min) / (max - min);
    }

    private double normalizeWeightDouble(Double value, Collection<Double> allValues) {
        double max = allValues.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double min = allValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        if (max == min) return 0.5;
        return 0.2 + 0.8 * (value - min) / (max - min);
    }
}
