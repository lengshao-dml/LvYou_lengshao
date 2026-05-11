package com.textoasis.service;

import com.textoasis.model.City;
import com.textoasis.model.CityPopularity;
import com.textoasis.repository.CityRepository;
import com.textoasis.repository.CityPopularityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularityService {

    private final CityRepository cityRepository;
    private final CityPopularityRepository popularityRepository;

    public void incrementClickCount(Long cityId) {
        log.info("Incrementing click count for cityId: {}", cityId);
        CityPopularity popularity = popularityRepository.findByCityId(cityId)
                .orElseGet(() -> createNewPopularityEntry(cityId));
        popularity.setClickCount(popularity.getClickCount() + 1);
        popularityRepository.save(popularity);
    }
    
    public void incrementSearchCount(String keyword) {
        // 尝试通过关键词精确匹配城市名称
        cityRepository.findByName(keyword).ifPresent(city -> {
            log.info("Incrementing search count for city: {}", city.getName());
            CityPopularity popularity = popularityRepository.findByCityId(city.getId())
                    .orElseGet(() -> createNewPopularityEntry(city.getId()));
            popularity.setSearchCount(popularity.getSearchCount() + 1);
            popularityRepository.save(popularity);
        });
    }

    private CityPopularity createNewPopularityEntry(Long cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid cityId: " + cityId));
        // 以景点数计算基础分（与 DataSeeder 逻辑一致）
        long totalAttractions = city.getFeatures() != null
                ? city.getFeatures().stream().mapToLong(f -> f.getAttractions() != null
                ? f.getAttractions().size() : 0).sum()
                : 0;
        double baseScore = Math.min(100.0, totalAttractions * 5.0);

        CityPopularity newPopularity = new CityPopularity();
        newPopularity.setCity(city);
        newPopularity.setBaseScore(baseScore);
        newPopularity.setScore(baseScore);
        return newPopularity;
    }

    @Transactional(readOnly = true)
    public List<CityPopularity> getPopularCities(int count) {
        return popularityRepository.findByOrderByScoreDesc(PageRequest.of(0, count));
    }

    @Scheduled(fixedRate = 15000) // 每15秒执行一次用于测试
    public void recalculateScores() {
        log.info("Starting scheduled job: recalculate popularity scores.");
        List<CityPopularity> allPopularities = popularityRepository.findAll();
        if (allPopularities.isEmpty()) {
            log.info("No city popularity data to recalculate. Skipping.");
            return;
        }

        for (CityPopularity popularity : allPopularities) {
            long searchCount = popularity.getSearchCount() != null ? popularity.getSearchCount() : 0;
            long clickCount = popularity.getClickCount() != null ? popularity.getClickCount() : 0;

            // 基础分占30%，用户行为分占70%
            double baseScore = popularity.getBaseScore() != null ? popularity.getBaseScore() : 0.0;
            double behaviorBoost = searchCount * 5.0 + clickCount * 10.0;
            popularity.setScore(baseScore * 0.3 + behaviorBoost * 0.7);
        }

        popularityRepository.saveAll(allPopularities);
        log.info("Finished recalculating scores for {} cities.", allPopularities.size());
    }
}
