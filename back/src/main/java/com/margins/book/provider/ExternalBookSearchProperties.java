package com.margins.book.provider;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "margins.book-search")
public class ExternalBookSearchProperties {
    private boolean enabled = true;
    private boolean aiFallbackEnabled = false;
    private String provider = "google";
    private String baseUrl = "https://openlibrary.org";
    private String googleBaseUrl = "https://www.googleapis.com";
    private String googleApiKey = "";
    private int timeoutSeconds = 5;
    private int limit = 5;
}
