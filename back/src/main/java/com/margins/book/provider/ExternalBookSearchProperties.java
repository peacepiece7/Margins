package com.margins.book.provider;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "margins.book-search")
public class ExternalBookSearchProperties {
    private boolean enabled = true;
    private String provider = "openlibrary";
    private String baseUrl = "https://openlibrary.org";
    private String kakaoBaseUrl = "https://dapi.kakao.com";
    private String kakaoRestApiKey = "";
    private int timeoutSeconds = 5;
    private int limit = 5;
}
