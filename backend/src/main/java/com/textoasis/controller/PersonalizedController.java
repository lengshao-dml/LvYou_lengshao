package com.textoasis.controller;

import com.textoasis.dto.PersonalizedRecommendationDto;
import com.textoasis.model.User;
import com.textoasis.repository.UserRepository;
import com.textoasis.service.PersonalizedRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PersonalizedController {

    private final PersonalizedRecommendationService personalizedService;
    private final UserRepository userRepository;

    /**
     * 获取个性化主页数据：猜你喜欢 + 热门标签词云 + 热门城市词云
     */
    @GetMapping("/personalized")
    public ResponseEntity<PersonalizedRecommendationDto> getPersonalized(Principal principal) {
        Optional<User> userOpt = Optional.empty();
        if (principal != null) {
            userOpt = userRepository.findByUsernameWithInterests(principal.getName());
        }
        PersonalizedRecommendationDto data = personalizedService.getPersonalizedData(userOpt);
        return ResponseEntity.ok(data);
    }
}
