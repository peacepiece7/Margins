package com.margins.book.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.margins.book.dto.BookCandidateDto;
import com.margins.book.dto.BookCandidateSearchResponse;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "margins.book-search.open-library.enabled", havingValue = "true", matchIfMissing = true)
public class OpenLibraryBookSearchProvider implements BookSearchProvider {

    private static final int MAX_RESULTS = 5;

    private final ObjectMapper objectMapper;

    @Override
    public BookCandidateSearchResponse search(String query) {
        if (query == null || query.isBlank()) {
            return empty();
        }

        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openlibrary.org/search.json?q=" + encodedQuery + "&limit=" + MAX_RESULTS))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return empty();
            }

            return BookCandidateSearchResponse.builder()
                .aiModel("open-library")
                .candidates(parseCandidates(response.body()))
                .build();
        } catch (IOException exception) {
            return empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return empty();
        } catch (RuntimeException exception) {
            return empty();
        }
    }

    private BookCandidateSearchResponse empty() {
        return BookCandidateSearchResponse.builder()
            .aiModel("open-library")
            .candidates(List.of())
            .build();
    }

    private List<BookCandidateDto> parseCandidates(String body) throws IOException {
        JsonNode docs = objectMapper.readTree(body).path("docs");
        List<BookCandidateDto> candidates = new ArrayList<>();
        if (!docs.isArray()) {
            return candidates;
        }

        for (JsonNode item : docs) {
            String key = item.path("key").asText("");
            String title = item.path("title").asText("");
            String author = item.path("author_name").isArray() && item.path("author_name").size() > 0
                ? item.path("author_name").get(0).asText("")
                : "";
            if (key.isBlank() || title.isBlank() || author.isBlank()) {
                continue;
            }

            Integer firstPublishYear = item.hasNonNull("first_publish_year")
                ? item.path("first_publish_year").asInt()
                : null;
            candidates.add(BookCandidateDto.builder()
                .candidateId("open-library:" + key)
                .title(title)
                .author(author)
                .publishedYear(firstPublishYear)
                .reason(firstPublishYear == null
                    ? "Open Library catalog result"
                    : "Open Library catalog result, first published " + firstPublishYear)
                .build());
        }
        return candidates;
    }
}
