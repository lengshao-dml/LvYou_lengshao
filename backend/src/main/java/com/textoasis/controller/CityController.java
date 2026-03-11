package com.textoasis.controller;

import com.textoasis.dto.CityDto;
import com.textoasis.dto.PopularCityDto;
import com.textoasis.mapper.CityMapper;
import com.textoasis.mapper.PopularityMapper;
import com.textoasis.model.City;
import com.textoasis.model.User;
import com.textoasis.repository.CityRepository;
import com.textoasis.repository.UserRepository;
import com.textoasis.service.PopularityService;
import com.textoasis.service.UserActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CityController {

    private final CityRepository cityRepository;
    private final UserRepository userRepository;
    private final UserActivityLogService logService;
    private final PopularityService popularityService;
    private final CityMapper cityMapper;
    private final PopularityMapper popularityMapper;

    @GetMapping("/cities")
    public ResponseEntity<List<CityDto>> getAllCities() {
        List<CityDto> cityDtos = cityRepository.findAll().stream()
                .map(cityMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(cityDtos);
    }

    @GetMapping("/cities/popular")
    public ResponseEntity<List<PopularCityDto>> getPopularCities(@RequestParam(defaultValue = "10") int count) {
        List<PopularCityDto> popularCities = popularityService.getPopularCities(count).stream()
                .map(popularityMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(popularCities);
    }

    @GetMapping("/city/{name}")
    public ResponseEntity<List<CityDto>> getCityByName(@PathVariable String name, Principal principal) {
        // 记录搜索行为 (异步)
        User user = null;
        if (principal != null) {
            user = userRepository.findByUsername(principal.getName()).orElse(null);
        }
        logService.logSearch(user, name);

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
