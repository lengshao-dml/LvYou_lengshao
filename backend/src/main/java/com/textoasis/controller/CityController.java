package com.textoasis.controller;

import com.textoasis.dto.CityDto;
import com.textoasis.mapper.CityMapper;
import com.textoasis.model.City;
import com.textoasis.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    @GetMapping("/city/{name}")
    public ResponseEntity<List<CityDto>> getCityByName(@PathVariable String name) {
        Set<City> results = new LinkedHashSet<>();

        // 1. 尝试精确匹配
        cityRepository.findByName(name).ifPresent(results::add);

        // 2. 如果结果为空，尝试添加“市”再精确匹配
        if (results.isEmpty() && !name.endsWith("市")) {
            cityRepository.findByName(name + "市").ifPresent(results::add);
        }

        // 3. 如果结果仍为空，执行模糊查询
        if (results.isEmpty()) {
            results.addAll(cityRepository.findByNameContaining(name));
        }

        if (results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<CityDto> cityDtos = results.stream()
                .map(cityMapper::toDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(cityDtos);
    }
}
