package com.warehouse.service;

import com.warehouse.dto.request.LoginRequest;
import com.warehouse.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    void logout(String token);
}
