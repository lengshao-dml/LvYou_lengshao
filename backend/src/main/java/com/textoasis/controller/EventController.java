package com.textoasis.controller;

import com.textoasis.dto.ClickEventDto;
import com.textoasis.model.User;
import com.textoasis.repository.CityRepository;
import com.textoasis.repository.UserRepository;
import com.textoasis.service.UserActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final UserActivityLogService logService;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;

    @PostMapping("/click")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logClickEvent(@RequestBody ClickEventDto clickEvent, Principal principal) {
        // principal is guaranteed to be non-null here due to @PreAuthorize
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        cityRepository.findById(clickEvent.getCityId()).ifPresent(city -> {
            logService.logClick(user, city);
        });

        return ResponseEntity.ok().build();
    }
}
