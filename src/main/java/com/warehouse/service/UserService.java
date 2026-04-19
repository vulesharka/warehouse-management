package com.warehouse.service;

import com.warehouse.dto.request.UserRequest;
import com.warehouse.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse createUser(UserRequest request);
    UserResponse updateUser(Long id, UserRequest request);
    void deleteUser(Long id, String currentUsername);
}
