package com.margins.auth.business;

import com.margins.auth.dto.LoginRequest;
import com.margins.auth.dto.LoginResponse;
import com.margins.auth.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthBusiness {

    private static final Long SINGLE_USER_ID = 1L;

    private final JwtTokenService jwtTokenService;

    public LoginResponse loginSingleUser(LoginRequest request) {
        String username = request.getUsername() == null || request.getUsername().isBlank()
            ? "test-reader"
            : request.getUsername();

        return LoginResponse.builder()
            .userId(SINGLE_USER_ID)
            .username(username)
            .displayName("Test Reader")
            .authMode("single-user-jwt")
            .accessToken(jwtTokenService.createToken(SINGLE_USER_ID, username))
            .build();
    }
}
