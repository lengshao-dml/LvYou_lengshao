package com.textoasis.dto;

import lombok.Data;

import java.util.List;

/**
 * 个性化推荐结果（包含推荐城市、热门标签词云、热门城市词云）
 */
@Data
public class PersonalizedRecommendationDto {
    /** 个性化推荐城市列表（"猜你喜欢"） */
    private List<RecommendationDto> recommendedCities;

    /** 热门标签词云数据 */
    private List<WordCloudItem> hotTags;

    /** 热门城市词云数据 */
    private List<WordCloudItem> hotCities;

    @Data
    public static class WordCloudItem {
        private String name;    // 标签或城市名称
        private double weight;  // 权重（用于词云渲染大小）
        private Long count;     // 原始计数
    }
}
