package com.textoasis.service;

import com.textoasis.model.City;
import com.textoasis.model.WeatherForecast;
import com.textoasis.repository.WeatherForecastRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherForecastRepository weatherForecastRepository;

    /**
     * 获取指定城市在某一天的天气预报文本。
     * @param city 城市实体
     * @param travelDate 日期
     * @return 天气预报字符串, e.g., "多云"
     */
    public String getWeatherText(City city, Date travelDate) {
        return getWeatherForecast(city, travelDate)
                .map(WeatherForecast::getText)
                .orElse("未知");
    }

    /**
     * 获取指定城市在某一天的完整天气预报对象。
     * @param city 城市实体
     * @param travelDate 日期
     * @return 天气预报对象 Optional
     */
    public Optional<WeatherForecast> getWeatherForecast(City city, Date travelDate) {
        // 在实际应用中，由于数据库存储的是 java.sql.Date (只有日期),
        // 而传入的 travelDate 可能是 java.util.Date (包含时间),
        // 查询前最好进行日期标准化，确保时间部分被忽略。
        // 为简化，此处假设 travelDate 的时间部分已经是 00:00:00。
        return weatherForecastRepository.findByCityAndForecastDate(city, travelDate);
    }

    /**
     * 获取指定城市全部15天天气预报（按日期排序）。
     */
    public List<WeatherForecast> getForecastsByCity(City city) {
        return weatherForecastRepository.findByCityOrderByForecastDateAsc(city);
    }
}
