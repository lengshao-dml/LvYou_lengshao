package com.textoasis.service;

import com.textoasis.model.City;
import com.textoasis.model.User;
import com.textoasis.model.UserClickHistory;
import com.textoasis.model.UserRecommendLog;
import com.textoasis.model.UserSearchHistory;
import com.textoasis.repository.UserClickHistoryRepository;
import com.textoasis.repository.UserRecommendLogRepository;
import com.textoasis.repository.UserSearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserActivityLogService {

    private final UserSearchHistoryRepository searchHistoryRepository;
    private final UserClickHistoryRepository clickHistoryRepository;
    private final UserRecommendLogRepository recommendLogRepository;
    private final PopularityService popularityService;
    private final UserInterestService userInterestService;

    public void logSearch(User user, String keyword) {
        UserSearchHistory log = new UserSearchHistory();
        log.setUser(user); // user 可以是 null
        log.setKeyword(keyword);
        searchHistoryRepository.save(log);

        // 更新城市搜索次数
        popularityService.incrementSearchCount(keyword);
    }

    public void logClick(User user, City city) {
        UserClickHistory log = new UserClickHistory();
        log.setUser(user); // user 可以是 null
        log.setCity(city);
        clickHistoryRepository.save(log);

        // 更新城市点击次数
        popularityService.incrementClickCount(city.getId());

        // 如果是登录用户，则更新兴趣权重（隐式反馈）
        if (user != null) {
            userInterestService.updateUserInterestsFromClick(user, city);
        }
    }

    public void logRecommend(User user, List<String> interestTags, String distanceScope) {
        UserRecommendLog log = new UserRecommendLog();
        log.setUser(user); // user 可以是 null
        log.setInterestTags(String.join(",", interestTags));
        log.setDistanceScope(distanceScope);
        recommendLogRepository.save(log);
    }
}
