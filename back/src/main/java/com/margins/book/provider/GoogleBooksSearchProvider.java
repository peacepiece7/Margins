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
public class GoogleBooksSearchProvider implements ExternalBookSearchProvider {

    private final ExternalBookSearchProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Override
    public String providerName() {
        return "google";
    }

    @Override
    public List<BookCandidateDto> search(String query) {
        if (!properties.isEnabled()) {
            log.info("Google Books search skipped because external book search is disabled");
            return List.of();
        }
        if (query == null || query.isBlank()) {
            log.info("Google Books search skipped because query is blank");
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
                log.warn("Google Books search failed. status={}, queryLength={}", response.statusCode(), query.trim().length());
                return List.of();
            }

            List<BookCandidateDto> candidates = parseCandidates(response.body());
            log.info("Google Books search completed. status={}, candidateCount={}, queryLength={}, requestQuery={}",
                response.statusCode(),
                candidates.size(),
                query.trim().length(),
                redactedRawQuery(request.uri()));
            return candidates;
        } catch (IOException exception) {
            log.warn("Google Books search failed with I/O error. queryLength={}, error={}",
                query.trim().length(),
                exception.getClass().getSimpleName());
            return List.of();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Google Books search interrupted. queryLength={}", query.trim().length());
            return List.of();
        } catch (RuntimeException exception) {
            log.warn("Google Books search failed with runtime error. queryLength={}, error={}",
                query.trim().length(),
                exception.getClass().getSimpleName());
            return List.of();
        }
    }

    private URI searchUri(String query) {
        int limit = Math.max(1, Math.min(properties.getLimit(), 10));
        String encodedQuery = URLEncoder.encode(googleQuery(query), StandardCharsets.UTF_8);
        String apiKey = properties.getGoogleApiKey() == null ? "" : properties.getGoogleApiKey().trim();
        String keyQuery = apiKey.isBlank()
            ? ""
            : "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        return URI.create(properties.getGoogleBaseUrl()
            + "/books/v1/volumes?q="
            + encodedQuery
            + "&maxResults="
            + limit
            + "&printType=books"
            + keyQuery);
    }

    private String redactedRawQuery(URI uri) {
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null) {
            return "";
        }
        return rawQuery.replaceAll("(?i)(^|&)key=[^&]*", "$1key=<redacted>");
    }

    private String googleQuery(String query) {
        String trimmed = query.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("intitle:") || lower.startsWith("inauthor:")) {
            return trimmed;
        }
        if (lower.startsWith("isbn:")) {
            String isbn = normalizedIsbn(trimmed);
            return isbn.isBlank() ? trimmed : "isbn:" + isbn;
        }
        if (lower.startsWith("title:")) {
            return "intitle:" + trimmed.substring("title:".length()).trim();
        }
        if (lower.startsWith("author:")) {
            return "inauthor:" + trimmed.substring("author:".length()).trim();
        }

        String isbn = normalizedIsbn(trimmed);
        return isbn.isBlank() ? trimmed : "isbn:" + isbn;
    }

    private List<BookCandidateDto> parseCandidates(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode items = root.path("items");
        if (!items.isArray()) {
            log.info("Google Books search response did not contain items array. totalItems={}", root.path("totalItems").asInt(0));
            return List.of();
        }

        List<BookCandidateDto> candidates = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode volumeInfo = item.path("volumeInfo");
            String title = text(volumeInfo, "title");
            String author = authorLabel(volumeInfo.path("authors"));
            if (title.isBlank()) {
                log.info("Google Books search ignored unusable item. hasId={}, hasTitle={}, authorCount={}",
                    !text(item, "id").isBlank(),
                    false,
                    volumeInfo.path("authors").isArray() ? volumeInfo.path("authors").size() : 0);
                continue;
            }

            String isbn10 = isbn(volumeInfo.path("industryIdentifiers"), "ISBN_10");
            String isbn13 = isbn(volumeInfo.path("industryIdentifiers"), "ISBN_13");
            String preferredIsbn = !isbn13.isBlank() ? isbn13 : isbn10;
            String googleId = text(item, "id");
            String candidateId = !preferredIsbn.isBlank() ? preferredIsbn : googleId;
            if (candidateId.isBlank()) {
                log.info("Google Books search ignored item without id or ISBN. titleLength={}", title.length());
                continue;
            }

            candidates.add(BookCandidateDto.builder()
                .candidateId("google:" + candidateId)
                .isbn(preferredIsbn.isBlank() ? null : preferredIsbn)
                .isbn10(isbn10.isBlank() ? null : isbn10)
                .isbn13(isbn13.isBlank() ? null : isbn13)
                .title(title)
                .subtitle(blankToNull(text(volumeInfo, "subtitle")))
                .author(author.isBlank() ? "Unknown author" : author)
                .authors(authors(volumeInfo.path("authors")))
                .publisher(blankToNull(text(volumeInfo, "publisher")))
                .publishedDate(blankToNull(text(volumeInfo, "publishedDate")))
                .publishedYear(publishedYear(text(volumeInfo, "publishedDate")))
                .description(blankToNull(text(volumeInfo, "description")))
                .thumbnail(thumbnail(volumeInfo.path("imageLinks")))
                .language(blankToNull(text(volumeInfo, "language")))
                .pageCount(volumeInfo.path("pageCount").isInt() ? volumeInfo.path("pageCount").asInt() : null)
                .reason(reason(volumeInfo))
                .build());
        }
        log.info("Google Books search parsed response. itemCount={}, candidateCount={}, totalItems={}",
            items.size(),
            candidates.size(),
            root.path("totalItems").asInt(-1));
        return candidates;
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("").trim();
    }

    private List<String> authors(JsonNode authors) {
        if (!authors.isArray() || authors.isEmpty()) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (JsonNode author : authors) {
            String name = author.asText("").trim();
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private String authorLabel(JsonNode authors) {
        return String.join(", ", authors(authors));
    }

    private String isbn(JsonNode identifiers, String type) {
        if (!identifiers.isArray()) {
            return "";
        }
        for (JsonNode identifier : identifiers) {
            if (type.equals(identifier.path("type").asText(""))) {
                return identifier.path("identifier").asText("").trim();
            }
        }
        return "";
    }

    private String normalizedIsbn(String query) {
        String normalized = query.toLowerCase().startsWith("isbn:")
            ? query.substring("isbn:".length()).trim()
            : query;
        normalized = normalized.replaceAll("[\\s-]", "");
        if (normalized.matches("(?i)\\d{9}[\\dX]|\\d{13}")) {
            return normalized.toUpperCase();
        }
        return "";
    }

    private Integer publishedYear(String publishedDate) {
        if (publishedDate == null || publishedDate.length() < 4) {
            return null;
        }

        try {
            return Integer.valueOf(publishedDate.substring(0, 4));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String thumbnail(JsonNode imageLinks) {
        String thumbnail = text(imageLinks, "thumbnail");
        if (!thumbnail.isBlank()) {
            return thumbnail;
        }
        return blankToNull(text(imageLinks, "smallThumbnail"));
    }

    private String reason(JsonNode volumeInfo) {
        List<String> parts = new ArrayList<>();
        parts.add("Google Books search result");
        String publisher = text(volumeInfo, "publisher");
        if (!publisher.isBlank()) {
            parts.add(publisher);
        }
        String publishedDate = text(volumeInfo, "publishedDate");
        if (!publishedDate.isBlank()) {
            parts.add(publishedDate);
        }
        return String.join(" - ", parts);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
