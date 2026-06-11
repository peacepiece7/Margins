package com.margins.auth.business;

import com.margins.auth.dto.LoginRequest;
import com.margins.auth.dto.LoginResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthBusiness {

    public LoginResponse loginSingleUser(LoginRequest request) {
        String username = request.getUsername() == null || request.getUsername().isBlank()
            ? "test-reader"
            : request.getUsername();

        return LoginResponse.builder()
            .userId(1L)
            .username(username)
            .displayName("Test Reader")
            .authMode("single-user")
            .accessToken("single-user-dev-token")
            .build();
    }
}
