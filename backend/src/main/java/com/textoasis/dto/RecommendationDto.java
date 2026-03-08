package com.textoasis.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
public class RecommendationDto {
    private Long cityId;
    private String name;
    private String province;
    private double score;
    private double distanceKm;
    private String weatherForecast;
    private Set<String> matchedTags;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
