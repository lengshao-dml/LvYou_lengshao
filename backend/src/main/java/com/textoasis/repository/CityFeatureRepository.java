package com.textoasis.repository;

import com.textoasis.model.City;
import com.textoasis.model.CityFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityFeatureRepository extends JpaRepository<CityFeature, Long> {
    List<CityFeature> findByCity(City city);
}
