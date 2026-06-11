package com.margins.auth.service;

import com.margins.auth.business.AuthBusiness;
import com.margins.auth.dto.LoginRequest;
import com.margins.auth.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthBusiness authBusiness;

    public LoginResponse login(LoginRequest request) {
        return authBusiness.loginSingleUser(request);
    }
}
