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
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class KakaoBookSearchProvider implements ExternalBookSearchProvider {

    private final ExternalBookSearchProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Override
    public String providerName() {
        return "kakao";
    }

    @Override
    public List<BookCandidateDto> search(String query) {
        if (!properties.isEnabled()) {
            log.info("Kakao book search skipped because external book search is disabled");
            return List.of();
        }
        if (query == null || query.isBlank()) {
            log.info("Kakao book search skipped because query is blank");
            return List.of();
        }
        if (properties.getKakaoRestApiKey().isBlank()) {
            log.warn("Kakao book search skipped because KAKAO_REST_API_KEY is missing");
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
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Kakao book search failed. status={}, queryLength={}, keyLength={}, keyHasWhitespace={}, keyStartsWithKakaoAK={}, errorCode={}, errorMessage={}",
                    response.statusCode(),
                    query.trim().length(),
                    properties.getKakaoRestApiKey().length(),
                    containsWhitespace(properties.getKakaoRestApiKey()),
                    properties.getKakaoRestApiKey().startsWith("KakaoAK"),
                    kakaoErrorCode(response.body()),
                    kakaoErrorMessage(response.body()));
                return List.of();
            }

            List<BookCandidateDto> candidates = parseCandidates(response.body());
            log.info("Kakao book search completed. status={}, candidateCount={}, queryLength={}, queryPreview={}, requestQuery={}",
                response.statusCode(),
                candidates.size(),
                query.trim().length(),
                queryPreview(query),
                request.uri().getRawQuery());
            return candidates;
        } catch (IOException exception) {
            log.warn("Kakao book search failed with I/O error. queryLength={}, error={}",
                query.trim().length(),
                exception.getClass().getSimpleName());
            return List.of();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Kakao book search interrupted. queryLength={}", query.trim().length());
            return List.of();
        } catch (RuntimeException exception) {
            log.warn("Kakao book search failed with runtime error. queryLength={}, error={}",
                query.trim().length(),
                exception.getClass().getSimpleName());
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
        JsonNode root = objectMapper.readTree(body);
        JsonNode documents = root.path("documents");
        if (!documents.isArray()) {
            log.warn("Kakao book search response did not contain documents array");
            return List.of();
        }

        List<BookCandidateDto> candidates = new ArrayList<>();
        int documentCount = documents.size();
        for (JsonNode item : documents) {
            String title = stripHtml(item.path("title").asText(""));
            String author = authors(item.path("authors"));
            String isbn = preferredIsbn(item);
            String candidateId = candidateId(item);
            if (candidateId.isBlank() || title.isBlank() || author.isBlank()) {
                log.info("Kakao book search ignored unusable document. hasCandidateId={}, hasTitle={}, hasAuthor={}, hasIsbn={}, hasUrl={}, authorCount={}",
                    !candidateId.isBlank(),
                    !title.isBlank(),
                    !author.isBlank(),
                    !isbn.isBlank(),
                    !item.path("url").asText("").isBlank(),
                    item.path("authors").isArray() ? item.path("authors").size() : 0);
                continue;
            }

            candidates.add(BookCandidateDto.builder()
                .candidateId("kakao:" + candidateId)
                .isbn(isbn)
                .title(title)
                .author(author)
                .publishedYear(publishedYear(item.path("datetime").asText("")))
                .reason(reason(item))
                .build());
        }
        log.info("Kakao book search parsed response. documentCount={}, candidateCount={}, totalCount={}, pageableCount={}, isEnd={}",
            documentCount,
            candidates.size(),
            root.path("meta").path("total_count").asInt(-1),
            root.path("meta").path("pageable_count").asInt(-1),
            root.path("meta").path("is_end").asBoolean(false));
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
        String isbn = preferredIsbn(item);
        if (!isbn.isBlank()) {
            return isbn;
        }

        return item.path("url").asText("").trim();
    }

    private String preferredIsbn(JsonNode item) {
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

        return "";
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

    private boolean containsWhitespace(String value) {
        return value != null && value.chars().anyMatch(Character::isWhitespace);
    }

    private String kakaoErrorCode(String body) {
        try {
            JsonNode code = objectMapper.readTree(body).path("code");
            return code.isMissingNode() ? "" : code.asText("");
        } catch (IOException | RuntimeException exception) {
            return "";
        }
    }

    private String kakaoErrorMessage(String body) {
        try {
            String message = objectMapper.readTree(body).path("msg").asText("");
            if (message.length() <= 160) {
                return message;
            }
            return message.substring(0, 160);
        } catch (IOException | RuntimeException exception) {
            return "";
        }
    }

    private String queryPreview(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() <= 40) {
            return trimmed;
        }
        return trimmed.substring(0, 40) + "...";
    }
}
