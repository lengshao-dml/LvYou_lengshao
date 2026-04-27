package com.textoasis.service;

import com.textoasis.config.QWeatherConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class QWeatherJwtGenerator {

    private final QWeatherConfig qWeatherConfig;

    public String generateJwt() {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        // Token valid for 15 minutes
        long expMillis = nowMillis + TimeUnit.MINUTES.toMillis(15);
        Date exp = new Date(expMillis);

        // Prepare JWT headers
        Map<String, Object> headers = new HashMap<>();
        headers.put("kid", qWeatherConfig.getKeyId());

        // Prepare JWT payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", qWeatherConfig.getPublicKey());
        payload.put("iat", now);
        payload.put("exp", exp);

        try {
            // Parse the EdDSA private key from the PEM format string
            String privateKeyPEM = qWeatherConfig.getPrivateKey()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll("\\n", "")
                    .replace("-----END PRIVATE KEY-----", "");

            byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("EdDSA");
            Key signingKey = keyFactory.generatePrivate(keySpec);

            // Build the JWT
            return Jwts.builder()
                    .setHeader(headers)
                    .setClaims(payload)
                    .signWith(signingKey)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Error generating QWeather JWT", e);
        }
    }
}
