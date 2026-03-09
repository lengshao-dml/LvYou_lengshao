package com.textoasis.dto;

import lombok.Data;

import java.util.List;

@Data
public class CityFeatureDto {
    private Long id;
    private String tagName;
    private List<AttractionDto> attractions;
}
