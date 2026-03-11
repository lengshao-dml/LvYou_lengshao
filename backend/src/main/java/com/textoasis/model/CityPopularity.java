package com.textoasis.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Data
@Entity
@Table(name = "city_popularity")
public class CityPopularity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false, unique = true)
    private City city;

    @Column(name = "search_count", nullable = false)
    private Long searchCount = 0L;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "score", nullable = false)
    private Double score = 0.0;

    @UpdateTimestamp
    @Column(name = "update_time")
    private Date updateTime;
}
