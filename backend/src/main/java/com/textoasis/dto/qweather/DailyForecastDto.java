package com.textoasis.dto.qweather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyForecastDto {
    private String fxDate;
    private String tempMax;
    private String tempMin;
    private String textDay;
    private String precip;
    private String vis;
    // Note: The API 'feelsLike' is for the current day, not in the daily forecast array.
    // We will have to handle this logic if we need daily feelsLike.
    // For now, we will use tempMax/tempMin.
}
