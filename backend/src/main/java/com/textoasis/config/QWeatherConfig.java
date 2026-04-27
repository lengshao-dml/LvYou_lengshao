package com.textoasis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "qweather.api")
@Data
public class QWeatherConfig {
    private String host;
    private String publicKey;
    private String privateKey;
    private String keyId;
}
