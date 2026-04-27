package com.textoasis.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Data
@Entity
@Table(name = "weather_forecasts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"city_id", "forecast_date"})
})
public class WeatherForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Temporal(TemporalType.DATE)
    @Column(name = "forecast_date", nullable = false)
    private Date forecastDate;

    @Column(name = "temp_max")
    private String tempMax;

    @Column(name = "temp_min")
    private String tempMin;

    @Column(name = "feels_like")
    private String feelsLike;

    @Column(name = "weather_text")
    private String text;

    @Column
    private String visibility;

    @Column
    private String precipitation;
    
    @UpdateTimestamp
    @Column(name = "update_time")
    private Date updateTime;
}
