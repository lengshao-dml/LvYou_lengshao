package com.textoasis.controller;

import com.textoasis.service.WeatherUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final WeatherUpdateService weatherUpdateService;

    @PostMapping("/trigger-weather-update")
    public ResponseEntity<String> triggerWeatherUpdate() {
        weatherUpdateService.updateAllCityWeatherForecasts(); // 直接调用服务，它内部会通过@Async处理异步
        
        return ResponseEntity.accepted().body("Weather forecast update job triggered. Check logs for progress.");
    }
}
