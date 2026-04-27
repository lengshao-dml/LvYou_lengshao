package com.textoasis.controller;

import com.textoasis.model.City;
import com.textoasis.model.WeatherForecast;
import com.textoasis.repository.CityRepository;
import com.textoasis.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/city")
@RequiredArgsConstructor
public class WeatherController {

    private final CityRepository cityRepository;
    private final WeatherService weatherService;

    /**
     * 获取指定城市的15天天气预报
     * GET /api/city/{name}/weather
     */
    @GetMapping("/{name}/weather")
    public ResponseEntity<List<WeatherForecastDto>> getCityWeather(@PathVariable String name) {
        // 复用 CityController 的查找逻辑：精确匹配 → 加"市" → 模糊匹配
        java.util.Optional<City> cityOpt = cityRepository.findByName(name);
        if (cityOpt.isEmpty() && !name.endsWith("市")) {
            cityOpt = cityRepository.findByName(name + "市");
        }
        if (cityOpt.isEmpty()) {
            List<City> results = cityRepository.findByNameContaining(name);
            if (!results.isEmpty()) {
                cityOpt = java.util.Optional.of(results.get(0));
            }
        }
        return cityOpt.map(city -> {
                    List<WeatherForecast> forecasts = weatherService.getForecastsByCity(city);
                    List<WeatherForecastDto> dtos = forecasts.stream()
                            .map(this::toDto)
                            .toList();
                    return ResponseEntity.ok(dtos);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private WeatherForecastDto toDto(WeatherForecast wf) {
        WeatherForecastDto dto = new WeatherForecastDto();
        dto.setForecastDate(wf.getForecastDate());
        dto.setTempMax(wf.getTempMax());
        dto.setTempMin(wf.getTempMin());
        dto.setText(wf.getText());
        dto.setPrecipitation(wf.getPrecipitation());
        dto.setVisibility(wf.getVisibility());
        return dto;
    }

    /**
     * 内部 DTO，不暴露 City 关联等内部细节
     */
    static class WeatherForecastDto {
        private java.util.Date forecastDate;
        private String tempMax;
        private String tempMin;
        private String text;
        private String precipitation;
        private String visibility;

        public java.util.Date getForecastDate() { return forecastDate; }
        public void setForecastDate(java.util.Date forecastDate) { this.forecastDate = forecastDate; }
        public String getTempMax() { return tempMax; }
        public void setTempMax(String tempMax) { this.tempMax = tempMax; }
        public String getTempMin() { return tempMin; }
        public void setTempMin(String tempMin) { this.tempMin = tempMin; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getPrecipitation() { return precipitation; }
        public void setPrecipitation(String precipitation) { this.precipitation = precipitation; }
        public String getVisibility() { return visibility; }
        public void setVisibility(String visibility) { this.visibility = visibility; }
    }
}
