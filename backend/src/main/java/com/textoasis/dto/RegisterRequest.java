package com.textoasis.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String nickname;
    private String homeCityName;
    private Map<String, Double> interests; // e.g., {"历史文化": 0.8, "自然风光": 0.5}
}
