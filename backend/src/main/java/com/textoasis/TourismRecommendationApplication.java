package com.textoasis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TourismRecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TourismRecommendationApplication.class, args);
    }

}
