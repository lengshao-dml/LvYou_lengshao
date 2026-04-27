package com.textoasis.controller;

import com.textoasis.dto.AuthenticationResponse;
import com.textoasis.dto.LoginRequest;
import com.textoasis.dto.RegisterRequest;
import com.textoasis.dto.UserProfileDto;
import com.textoasis.model.City;
import com.textoasis.model.User;
import com.textoasis.repository.CityRepository;
import com.textoasis.repository.UserRepository;
import com.textoasis.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        String token = authenticationService.register(request);
        return ResponseEntity.ok(new AuthenticationResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest request) {
        // Here we can create a temporary User object or have the service accept the DTO
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        String token = authenticationService.authenticate(user);
        return ResponseEntity.ok(new AuthenticationResponse(token));
    }

    /**
     * 获取当前登录用户信息（含常居城市名），用于前端个性化推荐默认值
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(null);
        }
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(null);
        }

        String homeCityName = null;
        if (user.getHomeCityId() != null) {
            City city = cityRepository.findById(user.getHomeCityId()).orElse(null);
            if (city != null) {
                homeCityName = city.getName();
            }
        }

        return ResponseEntity.ok(new UserProfileDto(user.getId(), user.getUsername(), homeCityName));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Hello World");
    }
}
