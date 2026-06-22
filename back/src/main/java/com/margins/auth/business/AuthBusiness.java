package com.margins.auth.business;

import com.margins.auth.config.SingleUserAuthProperties;
import com.margins.auth.dto.LoginRequest;
import com.margins.auth.dto.LoginResponse;
import com.margins.auth.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AuthBusiness {

    private static final Long SINGLE_USER_ID = 1L;

    private final JwtTokenService jwtTokenService;
    private final SingleUserAuthProperties singleUserAuthProperties;

    public LoginResponse loginSingleUser(LoginRequest request) {
        String username = request.getUsername().trim();
        if (!username.equals(singleUserAuthProperties.getUsername()) || !passwordMatches(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid username or password");
        }

        return LoginResponse.builder()
            .userId(SINGLE_USER_ID)
            .username(username)
            .displayName(singleUserAuthProperties.getDisplayName())
            .authMode("single-user-jwt")
            .accessToken(jwtTokenService.createToken(SINGLE_USER_ID, username))
            .build();
    }

    private boolean passwordMatches(String password) {
        String configuredPassword = singleUserAuthProperties.getPassword();
        if (configuredPassword == null || configuredPassword.isBlank()) {
            return false;
        }
        return configuredPassword.equals(password);
    }
}
