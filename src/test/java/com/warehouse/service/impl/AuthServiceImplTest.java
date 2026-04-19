package com.warehouse.service.impl;

import com.warehouse.dto.request.LoginRequest;
import com.warehouse.dto.response.AuthResponse;
import com.warehouse.security.JwtTokenProvider;
import com.warehouse.security.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider tokenProvider;
    @Mock TokenBlacklistService blacklistService;

    @InjectMocks AuthServiceImpl authService;

    @Test
    void login_returnsTokenOnValidCredentials() {
        UserDetails userDetails = new User("client", "encoded", List.of());
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, List.of());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(tokenProvider.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("client", "client123"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getType()).isEqualTo("Bearer");
    }

    @Test
    void logout_blacklistsToken() {
        authService.logout("some-token");
        verify(blacklistService).blacklist("some-token");
    }

    @Test
    void logout_doesNothingForNullToken() {
        authService.logout(null);
        verify(blacklistService, never()).blacklist(any());
    }
}
