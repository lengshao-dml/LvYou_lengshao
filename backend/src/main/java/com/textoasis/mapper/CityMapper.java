package com.textoasis.mapper;

import com.textoasis.dto.CityDto;
import com.textoasis.dto.CityFeatureDto;
import com.textoasis.dto.AttractionDto;
import com.textoasis.model.City;
import com.textoasis.model.CityFeature;
import com.textoasis.model.Attraction;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CityMapper {

    public CityDto toDto(City city) {
        if (city == null) {
            return null;
        }

        CityDto dto = new CityDto();
        dto.setId(city.getId());
        dto.setName(city.getName());
        dto.setProvince(city.getProvince());
        dto.setDescription(city.getDescription());
        dto.setLongitude(city.getLongitude());
        dto.setLatitude(city.getLatitude());
        dto.setHotnessScore(city.getHotnessScore());
        dto.setPinyin(city.getPinyin());
        dto.setAbbr(city.getAbbr());

        if (city.getFeatures() != null) {
            dto.setFeatures(city.getFeatures().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private CityFeatureDto toDto(CityFeature feature) {
        if (feature == null) {
            return null;
        }

        CityFeatureDto dto = new CityFeatureDto();
        dto.setId(feature.getId());
        if (feature.getTag() != null) {
            dto.setTagName(feature.getTag().getName());
        }

        if (feature.getAttractions() != null) {
            dto.setAttractions(feature.getAttractions().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private AttractionDto toDto(Attraction attraction) {
        if (attraction == null) {
            return null;
        }

        AttractionDto dto = new AttractionDto();
        dto.setId(attraction.getId());
        dto.setName(attraction.getName());
        dto.setDescription(attraction.getDescription());
        return dto;
    }
}
