package com.margins.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "margins.auth.jwt")
public class AuthJwtProperties {
    private String issuer = "margins";
    private String secret = "margins-local-dev-secret-change-before-production";
    private long ttlSeconds = 86400;
}
