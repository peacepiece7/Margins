package com.margins;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.margins.auth.config.AuthJwtProperties;
import com.margins.auth.filter.AuthTokenFilter;
import com.margins.auth.service.JwtTokenService;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthTokenFilterTest {

    @Test
    void rejectsProtectedApiWithoutBearerToken() throws ServletException, IOException {
        AuthTokenFilter filter = authFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reading-sessions/latest");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("unauthorized");
    }

    @Test
    void rejectsProtectedApiWithInvalidBearerToken() throws ServletException, IOException {
        AuthTokenFilter filter = authFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reading-sessions/latest");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void allowsProtectedApiWithValidBearerToken() throws ServletException, IOException {
        JwtTokenService tokenService = tokenService();
        AuthTokenFilter filter = new AuthTokenFilter(tokenService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reading-sessions/latest");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.createToken(1L, "test-reader"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(request.getAttribute(AuthTokenFilter.USER_ID_ATTRIBUTE)).isEqualTo(1L);
        assertThat(request.getAttribute(AuthTokenFilter.USERNAME_ATTRIBUTE)).isEqualTo("test-reader");
    }

    @Test
    void allowsLoginHealthAndTestResetWithoutBearerToken() throws ServletException, IOException {
        AuthTokenFilter filter = authFilter();

        assertOpenRoute(filter, "POST", "/api/auth/login");
        assertOpenRoute(filter, "GET", "/api/health");
        assertOpenRoute(filter, "POST", "/api/test/reset");
        assertOpenRoute(filter, "GET", "/v3/api-docs");
    }

    private void assertOpenRoute(AuthTokenFilter filter, String method, String path) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private AuthTokenFilter authFilter() {
        return new AuthTokenFilter(tokenService(), new ObjectMapper());
    }

    private JwtTokenService tokenService() {
        AuthJwtProperties properties = new AuthJwtProperties();
        properties.setIssuer("margins-test");
        properties.setSecret("test-secret");
        properties.setTtlSeconds(60);

        return new JwtTokenService(properties, new ObjectMapper());
    }
}
