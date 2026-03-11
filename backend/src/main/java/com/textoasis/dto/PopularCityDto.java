package com.textoasis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularCityDto {
    private Long cityId;
    private String name;
    private String province;
    private Double score;
}
