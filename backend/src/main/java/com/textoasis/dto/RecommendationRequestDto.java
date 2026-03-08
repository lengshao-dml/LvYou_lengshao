package com.textoasis.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecommendationRequestDto {
    private String departureCity;
    private List<String> interestTags;
    private String travelDate; // 格式暂定为 "YYYY-MM-DD"
    private String distanceScope; // 可选值: "PROVINCE", "NEARBY_500KM", "ANY"
}
