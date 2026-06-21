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
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class OpenLibraryBookSearchProvider implements ExternalBookSearchProvider {

    private final ExternalBookSearchProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Override
    public String providerName() {
        return "openlibrary";
    }

    @Override
    public List<BookCandidateDto> search(String query) {
        if (!properties.isEnabled()) {
            log.info("Open Library book search skipped because external book search is disabled");
            return List.of();
        }
        if (query == null || query.isBlank()) {
            log.info("Open Library book search skipped because query is blank");
            return List.of();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(searchUri(query))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Open Library book search failed. status={}, queryLength={}", response.statusCode(), query.trim().length());
                return List.of();
            }

            List<BookCandidateDto> candidates = parseCandidates(response.body());
            log.info("Open Library book search completed. status={}, candidateCount={}, queryLength={}",
                response.statusCode(),
                candidates.size(),
                query.trim().length());
            return candidates;
        } catch (IOException exception) {
            log.warn("Open Library book search failed with I/O error. queryLength={}, error={}",
                query.trim().length(),
                exception.getClass().getSimpleName());
            return List.of();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Open Library book search interrupted. queryLength={}", query.trim().length());
            return List.of();
        } catch (RuntimeException exception) {
            log.warn("Open Library book search failed with runtime error. queryLength={}, error={}",
                query.trim().length(),
                exception.getClass().getSimpleName());
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
            log.warn("Open Library book search response did not contain docs array");
            return List.of();
        }

        List<BookCandidateDto> candidates = new ArrayList<>();
        for (JsonNode item : docs) {
            String key = item.path("key").asText("");
            String title = item.path("title").asText("");
            String author = firstAuthor(item.path("author_name"));
            if (key.isBlank() || title.isBlank() || author.isBlank()) {
                log.info("Open Library book search ignored unusable document. hasKey={}, hasTitle={}, hasAuthor={}",
                    !key.isBlank(),
                    !title.isBlank(),
                    !author.isBlank());
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
