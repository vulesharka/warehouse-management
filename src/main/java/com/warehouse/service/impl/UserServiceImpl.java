package com.warehouse.service.impl;

import com.warehouse.dto.request.UserRequest;
import com.warehouse.dto.response.UserResponse;
import com.warehouse.entity.User;
import com.warehouse.exception.BusinessException;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.mapper.UserMapper;
import com.warehouse.repository.OrderRepository;
import com.warehouse.repository.UserRepository;
import com.warehouse.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(findById(id));
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("Password is required when creating a user");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already in use: " + request.getEmail());
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        User saved = userRepository.save(user);
        log.info("Created user: {} with role: {}", saved.getUsername(), saved.getRole());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = findById(id);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        log.info("Updated user: {}", user.getUsername());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id, String currentUsername) {
        User user = findById(id);
        if (user.getUsername().equals(currentUsername)) {
            throw new BusinessException("You cannot delete your own account");
        }
        if (orderRepository.existsByClientId(id)) {
            throw new BusinessException("Cannot delete user '" + user.getUsername() + "' because they have existing orders.");
        }
        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
