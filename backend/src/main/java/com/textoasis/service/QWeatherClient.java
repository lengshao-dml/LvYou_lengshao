package com.textoasis.service;

import com.textoasis.config.QWeatherConfig;
import com.textoasis.dto.qweather.QWeather15dResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class QWeatherClient {

    private final RestTemplate restTemplate;
    private final QWeatherConfig qWeatherConfig;
    private final QWeatherJwtGenerator qWeatherJwtGenerator;

    public QWeather15dResponseDto get15DayForecast(BigDecimal longitude, BigDecimal latitude) {
        // 格式化经纬度，保留两位小数
        String location = longitude.setScale(2, RoundingMode.HALF_UP) + "," + latitude.setScale(2, RoundingMode.HALF_UP);

        String url = UriComponentsBuilder.fromHttpUrl("https://" + qWeatherConfig.getHost() + "/v7/weather/15d")
                .queryParam("location", location)
                .toUriString();

        String jwt = qWeatherJwtGenerator.generateJwt();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwt);
        headers.set("Accept-Encoding", "gzip"); // 明确表示接受GZIP压缩
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            log.info("Requesting 15-day forecast for location: {}", location);
            ResponseEntity<QWeather15dResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    QWeather15dResponseDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && "200".equals(response.getBody().getCode())) {
                return response.getBody();
            } else if (response.getBody() != null) { // API调用成功，但和风返回了非200的业务代码
                log.error("Failed to get weather forecast for location {}. HTTP Status: {}, QWeather Code: {}", location, response.getStatusCode(), response.getBody().getCode());
                return null;
            } else { // HTTP状态码非2xx，或者响应体为空
                log.error("Failed to get weather forecast for location {}. HTTP Status: {}, Response Body is null.", location, response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Exception while fetching weather forecast for location {}", location, e);
            return null;
        }
    }
}
