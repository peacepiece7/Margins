package com.margins.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "margins.ai.openai")
public class OpenAiProperties {
    private String apiKey;
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-5.5";
    private int timeoutSeconds = 30;
}
