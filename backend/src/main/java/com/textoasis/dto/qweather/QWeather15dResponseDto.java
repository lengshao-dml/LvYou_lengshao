package com.textoasis.dto.qweather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QWeather15dResponseDto {
    private String code;
    private List<DailyForecastDto> daily;
}
