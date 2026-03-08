package com.textoasis.controller;

import com.textoasis.dto.RecommendationDto;
import com.textoasis.dto.RecommendationRequestDto;
import com.textoasis.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping("/recommend")
    public ResponseEntity<List<RecommendationDto>> recommend(@RequestBody RecommendationRequestDto request) {
        return ResponseEntity.ok(recommendationService.recommend(request));
    }
}
