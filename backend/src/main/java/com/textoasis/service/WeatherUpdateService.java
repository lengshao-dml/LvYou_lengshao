package com.textoasis.service;

import com.textoasis.dto.qweather.DailyForecastDto;
import com.textoasis.dto.qweather.QWeather15dResponseDto;
import com.textoasis.model.City;
import com.textoasis.model.WeatherForecast;
import com.textoasis.repository.CityRepository;
import com.textoasis.repository.WeatherForecastRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherUpdateService {

    private final CityRepository cityRepository;
    private final QWeatherClient qWeatherClient;
    private final WeatherForecastRepository weatherForecastRepository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // 启动后延迟 5 秒首次执行，然后每天凌晨 2 点更新
    @Async
    @Transactional
    @PostConstruct
    public void initWeatherData() {
        log.info("系统启动，开始首次加载天气数据...");
        updateAllCityWeatherForecasts();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Async
    @Transactional
    public void updateAllCityWeatherForecasts() {
        log.info("Starting scheduled job: Update all city weather forecasts.");
        List<City> cities = cityRepository.findAll();
        for (City city : cities) {
            try {
                QWeather15dResponseDto responseDto = qWeatherClient.get15DayForecast(city.getLongitude(), city.getLatitude());

                if (responseDto != null && responseDto.getDaily() != null) {
                    for (DailyForecastDto dailyDto : responseDto.getDaily()) {
                        saveOrUpdateForecast(city, dailyDto);
                    }
                }
                // API有速率限制，应该由RestTemplate或其配置处理，而不是在此处阻塞
            } catch (Exception e) {
                log.error("Failed to update weather for city: {}", city.getName(), e);
            }
        }
        log.info("Finished scheduled job: Update all city weather forecasts.");
    }

    private void saveOrUpdateForecast(City city, DailyForecastDto dailyDto) throws ParseException {
        Date forecastDate = dateFormat.parse(dailyDto.getFxDate());
        
        Optional<WeatherForecast> existingForecastOpt = weatherForecastRepository.findByCityAndForecastDate(city, forecastDate);
        
        WeatherForecast forecast = existingForecastOpt.orElse(new WeatherForecast());
        forecast.setCity(city);
        forecast.setForecastDate(forecastDate);
        forecast.setTempMax(dailyDto.getTempMax());
        forecast.setTempMin(dailyDto.getTempMin());
        forecast.setText(dailyDto.getTextDay());
        forecast.setPrecipitation(dailyDto.getPrecip());
        forecast.setVisibility(dailyDto.getVis());
        // 'feelsLike' is not in the 15d forecast, so we leave it null for now.
        
        weatherForecastRepository.save(forecast);
    }
}
