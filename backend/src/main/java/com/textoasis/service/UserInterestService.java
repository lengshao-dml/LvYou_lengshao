package com.textoasis.service;

import com.textoasis.model.*;
import com.textoasis.repository.CityFeatureRepository;
import com.textoasis.repository.UserInterestTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInterestService {

    private static final double WEIGHT_INCREMENT = 0.05;
    private static final double MAX_WEIGHT = 1.0;
    private static final double INITIAL_WEIGHT = 0.1;

    private final UserInterestTagRepository userInterestTagRepository;
    private final CityFeatureRepository cityFeatureRepository;

    @Transactional
    public void updateUserInterestsFromClick(User user, City city) {
        log.info("Updating interests for user '{}' based on click on city '{}'", user.getUsername(), city.getName());

        List<CityFeature> features = cityFeatureRepository.findByCity(city);
        if (features.isEmpty()) {
            return;
        }

        for (CityFeature feature : features) {
            Tag tag = feature.getTag();
            if (tag == null) continue;

            Optional<UserInterestTag> existingInterestOpt = userInterestTagRepository.findByUserAndTag(user, tag);

            if (existingInterestOpt.isPresent()) {
                // 如果存在，增加权重
                UserInterestTag existingInterest = existingInterestOpt.get();
                double newWeight = Math.min(MAX_WEIGHT, existingInterest.getWeight() + WEIGHT_INCREMENT);
                existingInterest.setWeight(newWeight);
                userInterestTagRepository.save(existingInterest);
                log.debug("Incremented weight for tag '{}' to {}", tag.getName(), newWeight);
            } else {
                // 如果不存在，创建新的兴趣标签
                UserInterestTag newInterest = new UserInterestTag();
                newInterest.setUser(user);
                newInterest.setTag(tag);
                newInterest.setWeight(INITIAL_WEIGHT);
                userInterestTagRepository.save(newInterest);
                log.debug("Created new interest for tag '{}' with initial weight {}", tag.getName(), INITIAL_WEIGHT);
            }
        }
    }
}
