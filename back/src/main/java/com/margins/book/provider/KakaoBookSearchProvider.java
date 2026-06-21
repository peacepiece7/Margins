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
@Order(10)
@RequiredArgsConstructor
public class KakaoBookSearchProvider implements ExternalBookSearchProvider {

    private final ExternalBookSearchProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public String providerName() {
        return "kakao";
    }

    @Override
    public List<BookCandidateDto> search(String query) {
        if (!properties.isEnabled() || query == null || query.isBlank() || properties.getKakaoRestApiKey().isBlank()) {
            return List.of();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(searchUri(query))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Accept", "application/json")
                .header("Authorization", "KakaoAK " + properties.getKakaoRestApiKey())
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
        return URI.create(properties.getKakaoBaseUrl()
            + "/v3/search/book?query="
            + encodedQuery
            + "&sort=accuracy&page=1&size="
            + limit);
    }

    private List<BookCandidateDto> parseCandidates(String body) throws IOException {
        JsonNode documents = objectMapper.readTree(body).path("documents");
        if (!documents.isArray()) {
            return List.of();
        }

        List<BookCandidateDto> candidates = new ArrayList<>();
        for (JsonNode item : documents) {
            String title = stripHtml(item.path("title").asText(""));
            String author = authors(item.path("authors"));
            String candidateId = candidateId(item);
            if (candidateId.isBlank() || title.isBlank() || author.isBlank()) {
                continue;
            }

            candidates.add(BookCandidateDto.builder()
                .candidateId("kakao:" + candidateId)
                .title(title)
                .author(author)
                .publishedYear(publishedYear(item.path("datetime").asText("")))
                .reason(reason(item))
                .build());
        }
        return candidates;
    }

    private String authors(JsonNode authors) {
        if (!authors.isArray() || authors.isEmpty()) {
            return "";
        }

        List<String> names = new ArrayList<>();
        for (JsonNode author : authors) {
            String name = author.asText("").trim();
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return String.join(", ", names);
    }

    private String candidateId(JsonNode item) {
        String isbn = item.path("isbn").asText("").trim();
        if (!isbn.isBlank()) {
            String[] parts = isbn.split("\\s+");
            for (String part : parts) {
                if (part.length() == 13) {
                    return part;
                }
            }
            return parts[0];
        }

        return item.path("url").asText("").trim();
    }

    private Integer publishedYear(String datetime) {
        if (datetime == null || datetime.length() < 4) {
            return null;
        }

        try {
            return Integer.valueOf(datetime.substring(0, 4));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String reason(JsonNode item) {
        List<String> parts = new ArrayList<>();
        parts.add("Kakao book search result");
        String publisher = item.path("publisher").asText("").trim();
        if (!publisher.isBlank()) {
            parts.add(publisher);
        }
        String status = item.path("status").asText("").trim();
        if (!status.isBlank()) {
            parts.add(status);
        }
        return String.join(" - ", parts);
    }

    private String stripHtml(String text) {
        return text == null ? "" : text.replaceAll("<[^>]*>", "").trim();
    }
}
