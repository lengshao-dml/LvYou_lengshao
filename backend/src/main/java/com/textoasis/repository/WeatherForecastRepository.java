package com.textoasis.repository;

import com.textoasis.model.City;
import com.textoasis.model.WeatherForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherForecastRepository extends JpaRepository<WeatherForecast, Long> {

    Optional<WeatherForecast> findByCityAndForecastDate(City city, Date forecastDate);

    List<WeatherForecast> findByCityOrderByForecastDateAsc(City city);

}
