package com.margins.book.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.margins.book.dto.BookCandidateDto;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
public class OpenLibraryBookSearchProvider implements ExternalBookSearchProvider {

    private final ExternalBookSearchProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public String providerName() {
        return "openlibrary";
    }

    @Override
    public List<BookCandidateDto> search(String query) {
        if (!properties.isEnabled() || query == null || query.isBlank()) {
            return List.of();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(searchUri(query))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }

            return parseCandidates(response.body());
        } catch (IOException exception) {
            return List.of();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private URI searchUri(String query) {
        int limit = Math.max(1, Math.min(properties.getLimit(), 10));
        String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        return URI.create(properties.getBaseUrl()
            + "/search.json?q="
            + encodedQuery
            + "&limit="
            + limit
            + "&fields=key,title,author_name,first_publish_year");
    }

    private List<BookCandidateDto> parseCandidates(String body) throws IOException {
        JsonNode docs = objectMapper.readTree(body).path("docs");
        if (!docs.isArray()) {
            return List.of();
        }

        List<BookCandidateDto> candidates = new ArrayList<>();
        for (JsonNode item : docs) {
            String key = item.path("key").asText("");
            String title = item.path("title").asText("");
            String author = firstAuthor(item.path("author_name"));
            if (key.isBlank() || title.isBlank() || author.isBlank()) {
                continue;
            }

            candidates.add(BookCandidateDto.builder()
                .candidateId("openlibrary:" + key)
                .title(title)
                .author(author)
                .publishedYear(item.path("first_publish_year").isInt() ? item.path("first_publish_year").asInt() : null)
                .reason("Open Library search result")
                .build());
        }
        return candidates;
    }

    private String firstAuthor(JsonNode authors) {
        if (!authors.isArray() || authors.isEmpty()) {
            return "";
        }
        return authors.get(0).asText("");
    }
}
