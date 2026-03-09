package com.textoasis.service;

import com.textoasis.model.City;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class WeatherService {

    private final Random random = new Random();

    /**
     * 获取指定城市在某一天的天气预报（模拟）。
     * 此方法的结果会被缓存。缓存的名称为 "weather"，Key 由 cityName 和 date 自动生成。
     * @param city 城市实体
     * @param date 日期字符串 (YYYY-MM-DD)
     * @return 模拟的天气预报字符串
     */
    @Cacheable(value = "weather", key = "#city.name + ':' + #date")
    public String getWeather(City city, String date) {
        // 模拟调用API的耗时
        try {
            Thread.sleep(500); // 模拟0.5秒延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 模拟返回随机天气类型
        String[] weathers = {"晴", "多云", "小雨", "大雨", "雪"};
        int index = random.nextInt(weathers.length);
        
        System.out.println("INFO: Calling MOCK Weather API for " + city.getName() + " on " + date + ". Result: " + weathers[index]);

        return weathers[index];
    }
}
