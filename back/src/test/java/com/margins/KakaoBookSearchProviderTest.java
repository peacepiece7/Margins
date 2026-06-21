package com.margins;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.margins.book.dto.BookCandidateDto;
import com.margins.book.provider.ExternalBookSearchProperties;
import com.margins.book.provider.KakaoBookSearchProvider;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class KakaoBookSearchProviderTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void searchMapsKakaoDocumentsToBookCandidates() throws Exception {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        AtomicReference<String> queryString = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/search/book", (exchange) -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            queryString.set(exchange.getRequestURI().getRawQuery());
            byte[] response = """
                {
                  "documents": [
                    {
                      "title": "<b>미움받을 용기</b>",
                      "authors": ["기시미 이치로", "고가 후미타케"],
                      "isbn": "8996991341 9788996991342",
                      "datetime": "2014-11-17T00:00:00.000+09:00",
                      "publisher": "인플루엔셜",
                      "status": "정상판매",
                      "thumbnail": "https://search1.kakaocdn.net/thumb/book.jpg",
                      "url": "https://search.daum.net/search?w=bookpage"
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(response);
            }
        });
        server.start();

        KakaoBookSearchProvider provider = new KakaoBookSearchProvider(properties("test-key"), new ObjectMapper(), HttpClient.newHttpClient());

        List<BookCandidateDto> candidates = provider.search("미움받을 용기");

        assertThat(provider.providerName()).isEqualTo("kakao");
        assertThat(authorizationHeader.get()).isEqualTo("KakaoAK test-key");
        assertThat(queryString.get()).contains("query=%EB%AF%B8%EC%9B%80%EB%B0%9B%EC%9D%84+%EC%9A%A9%EA%B8%B0");
        assertThat(candidates).singleElement()
            .satisfies((candidate) -> {
                assertThat(candidate.getCandidateId()).isEqualTo("kakao:9788996991342");
                assertThat(candidate.getIsbn()).isEqualTo("9788996991342");
                assertThat(candidate.getTitle()).isEqualTo("미움받을 용기");
                assertThat(candidate.getAuthor()).isEqualTo("기시미 이치로, 고가 후미타케");
                assertThat(candidate.getPublishedYear()).isEqualTo(2014);
                assertThat(candidate.getReason()).contains("Kakao book search result", "인플루엔셜", "정상판매");
            });
    }

    @Test
    void searchReturnsEmptyWithoutApiKey() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        KakaoBookSearchProvider provider = new KakaoBookSearchProvider(properties(""), new ObjectMapper(), HttpClient.newHttpClient());

        assertThat(provider.search("미움받을 용기")).isEmpty();
    }

    private ExternalBookSearchProperties properties(String apiKey) {
        ExternalBookSearchProperties properties = new ExternalBookSearchProperties();
        properties.setEnabled(true);
        properties.setKakaoBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setKakaoRestApiKey(apiKey);
        properties.setTimeoutSeconds(2);
        properties.setLimit(2);
        return properties;
    }
}
