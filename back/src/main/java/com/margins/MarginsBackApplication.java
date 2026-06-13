package com.margins;

import com.margins.ai.OpenAiProperties;
import com.margins.auth.config.AuthJwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({OpenAiProperties.class, AuthJwtProperties.class})
public class MarginsBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarginsBackApplication.class, args);
    }
}
