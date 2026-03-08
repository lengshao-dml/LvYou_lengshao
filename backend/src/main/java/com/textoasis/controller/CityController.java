package com.textoasis.controller;

import com.textoasis.dto.CityDto;
import com.textoasis.mapper.CityMapper;
import com.textoasis.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CityController {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;

    @GetMapping("/cities")
    public ResponseEntity<List<CityDto>> getAllCities() {
        List<CityDto> cityDtos = cityRepository.findAll().stream()
                .map(cityMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(cityDtos);
    }
}
