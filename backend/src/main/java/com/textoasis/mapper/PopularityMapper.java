package com.textoasis.mapper;

import com.textoasis.dto.PopularCityDto;
import com.textoasis.model.CityPopularity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PopularityMapper {

    @Mapping(source = "city.id", target = "cityId")
    @Mapping(source = "city.name", target = "name")
    @Mapping(source = "city.province", target = "province")
    @Mapping(source = "score", target = "score")
    PopularCityDto toDto(CityPopularity cityPopularity);
}
