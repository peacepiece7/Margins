package com.margins.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "margins.auth.single-user")
public class SingleUserAuthProperties {
    private String username = "peacepiece";
    private String displayName = "peacepiece";
    private String password;
}
