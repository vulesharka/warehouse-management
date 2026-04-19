package com.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.dto.request.UserRequest;
import com.warehouse.dto.response.UserResponse;
import com.warehouse.enums.Role;
import com.warehouse.exception.BusinessException;
import com.warehouse.exception.GlobalExceptionHandler;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserService userService;
    @InjectMocks UserController userController;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    private UserResponse userResponse;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver(), new PageableHandlerMethodArgumentResolver())
                .build();
        userResponse = new UserResponse(1L, "newuser", "new@test.com", Role.CLIENT, null);
        userRequest = new UserRequest("newuser", "new@test.com", "pass123", Role.CLIENT);

        User principal = new User("admin", "", List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsers_returns200WithList() throws Exception {
        when(userService.getAllUsers(any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(userResponse)));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("newuser"))
                .andExpect(jsonPath("$.content[0].role").value("CLIENT"));
    }

    @Test
    void getUser_returns200WhenFound() throws Exception {
        when(userService.getUserById(1L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void getUser_returns404WhenNotFound() throws Exception {
        when(userService.getUserById(99L))
                .thenThrow(new ResourceNotFoundException("User", 99L));

        mockMvc.perform(get("/api/admin/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUser_returns201() throws Exception {
        when(userService.createUser(any())).thenReturn(userResponse);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void createUser_returns400WhenBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_returns400WhenUsernameTaken() throws Exception {
        when(userService.createUser(any()))
                .thenThrow(new BusinessException("Username already taken"));

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_returns200() throws Exception {
        when(userService.updateUser(eq(1L), any())).thenReturn(userResponse);

        mockMvc.perform(put("/api/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void updateUser_returns404WhenNotFound() throws Exception {
        when(userService.updateUser(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("User", 99L));

        mockMvc.perform(put("/api/admin/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_returns204() throws Exception {
        doNothing().when(userService).deleteUser(eq(1L), eq("admin"));

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_returns400WhenDeletingOwnAccount() throws Exception {
        doThrow(new BusinessException("cannot delete your own account"))
                .when(userService).deleteUser(eq(1L), eq("admin"));

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_returns404WhenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User", 99L))
                .when(userService).deleteUser(eq(99L), eq("admin"));

        mockMvc.perform(delete("/api/admin/users/99"))
                .andExpect(status().isNotFound());
    }
}
