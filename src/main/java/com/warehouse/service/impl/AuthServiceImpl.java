package com.warehouse.service.impl;

import com.warehouse.dto.request.LoginRequest;
import com.warehouse.dto.response.AuthResponse;
import com.warehouse.security.JwtTokenProvider;
import com.warehouse.security.TokenBlacklistService;
import com.warehouse.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService blacklistService;

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = tokenProvider.generateToken(userDetails);
        log.info("User logged in: {}", request.getUsername());
        return new AuthResponse(token);
    }

    @Override
    public void logout(String token) {
        if (token != null) {
            blacklistService.blacklist(token);
            log.info("Token blacklisted on logout");
        }
    }
}
