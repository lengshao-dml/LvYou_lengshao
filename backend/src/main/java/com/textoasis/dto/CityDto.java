package com.textoasis.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CityDto {
    private Long id;
    private String name;
    private String province;
    private String description;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer hotnessScore;
    private String pinyin;
    private String abbr;
    private List<CityFeatureDto> features;
}
