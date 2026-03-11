package com.textoasis.service;

import com.textoasis.dto.RegisterRequest;
import com.textoasis.model.City;
import com.textoasis.model.Tag;
import com.textoasis.model.User;
import com.textoasis.model.UserInterestTag;
import com.textoasis.repository.CityRepository;
import com.textoasis.repository.TagRepository;
import com.textoasis.repository.UserRepository;
import com.textoasis.exception.RegistrationException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public String register(RegisterRequest request) {
        // 检查用户名是否已存在
        userRepository.findByUsername(request.getUsername()).ifPresent(u -> {
            throw new RegistrationException("该用户名已被使用");
        });
        // 检查邮箱是否已存在
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
                throw new RegistrationException("该邮箱已被注册");
            });
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());

        // 处理常居地
        if (request.getHomeCityName() != null && !request.getHomeCityName().isEmpty()) {
            cityRepository.findByName(request.getHomeCityName())
                .ifPresent(city -> user.setHomeCityId(city.getId()));
        }

        // 处理兴趣标签和权重
        Set<UserInterestTag> interestTags = new HashSet<>();
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            for (Map.Entry<String, Double> entry : request.getInterests().entrySet()) {
                tagRepository.findByName(entry.getKey()).ifPresent(tag -> {
                    UserInterestTag userInterestTag = new UserInterestTag();
                    userInterestTag.setUser(user);
                    userInterestTag.setTag(tag);
                    userInterestTag.setWeight(entry.getValue());
                    interestTags.add(userInterestTag);
                });
            }
        }
        user.setInterestTags(interestTags);

        userRepository.save(user);
        return jwtService.generateToken(user);
    }

    public String authenticate(User request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        return jwtService.generateToken(user);
    }
}
