package com.textoasis.controller;

import com.textoasis.dto.RecommendationDto;
import com.textoasis.dto.RecommendationRequestDto;
import com.textoasis.model.User;
import com.textoasis.repository.UserRepository;
import com.textoasis.service.RecommendationService;
import com.textoasis.service.UserActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserActivityLogService logService;
    private final UserRepository userRepository;

    @PostMapping("/recommend")
    public ResponseEntity<List<RecommendationDto>> recommend(@RequestBody RecommendationRequestDto request, Principal principal) {
        // 记录推荐请求行为 (异步)
        User user = null;
        if (principal != null) {
            user = userRepository.findByUsername(principal.getName()).orElse(null);
        }
        logService.logRecommend(user, request.getInterestTags(), request.getDistanceScope());

        return ResponseEntity.ok(recommendationService.recommend(request, Optional.ofNullable(user)));
    }
}
