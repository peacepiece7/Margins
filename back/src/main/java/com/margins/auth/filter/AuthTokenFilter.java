package com.margins.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.margins.auth.service.JwtTokenService;
import com.margins.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTRIBUTE = "margins.userId";
    public static final String USERNAME_ATTRIBUTE = "margins.username";

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return HttpMethod.OPTIONS.matches(request.getMethod())
            || !path.startsWith("/api/")
            || path.equals("/api/auth/login")
            || path.equals("/api/health")
            || path.startsWith("/api/test/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return;
        }

        jwtTokenService.validate(header.substring("Bearer ".length()))
            .ifPresentOrElse(principal -> {
                request.setAttribute(USER_ID_ATTRIBUTE, principal.getUserId());
                request.setAttribute(USERNAME_ATTRIBUTE, principal.getUsername());
            }, () -> request.setAttribute(USER_ID_ATTRIBUTE, null));

        if (request.getAttribute(USER_ID_ATTRIBUTE) == null) {
            writeUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) {
        try {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponse.failed("unauthorized"));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write unauthorized response", exception);
        }
    }
}
