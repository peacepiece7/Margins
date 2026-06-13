package com.margins.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.margins.auth.config.AuthJwtProperties;
import com.margins.auth.dto.AuthPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";
    private static final String JWT_TYPE = "JWT";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AuthJwtProperties properties;
    private final ObjectMapper objectMapper;

    public String createToken(Long userId, String username) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = Map.of(
            "alg", JWT_ALGORITHM,
            "typ", JWT_TYPE
        );
        Map<String, Object> payload = Map.of(
            "iss", properties.getIssuer(),
            "sub", username,
            "userId", userId,
            "iat", now,
            "exp", now + properties.getTtlSeconds()
        );
        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);

        return unsignedToken + "." + sign(unsignedToken);
    }

    public Optional<AuthPrincipal> validate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            String unsignedToken = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsignedToken).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }

            Map<String, Object> header = objectMapper.readValue(decode(parts[0]), MAP_TYPE);
            if (!JWT_ALGORITHM.equals(header.get("alg")) || !JWT_TYPE.equals(header.get("typ"))) {
                return Optional.empty();
            }

            Map<String, Object> payload = objectMapper.readValue(decode(parts[1]), MAP_TYPE);
            if (!properties.getIssuer().equals(payload.get("iss"))) {
                return Optional.empty();
            }
            if (numberValue(payload.get("exp")) < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            return Optional.of(AuthPrincipal.builder()
                .userId(numberValue(payload.get("userId")))
                .username(String.valueOf(payload.get("sub")))
                .build());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode JWT payload", exception);
        }
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT", exception);
        }
    }

    private Long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(String.valueOf(value));
    }
}
