package com.warehouse.service.impl;

import com.warehouse.dto.request.UserRequest;
import com.warehouse.dto.response.UserResponse;
import com.warehouse.entity.User;
import com.warehouse.enums.Role;
import com.warehouse.exception.BusinessException;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.mapper.UserMapper;
import com.warehouse.repository.OrderRepository;
import com.warehouse.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock OrderRepository orderRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserMapper userMapper;

    @InjectMocks UserServiceImpl userService;

    @Test
    void createUser_encodesPasswordAndSaves() {
        UserRequest request = new UserRequest("newuser", "new@test.com", "pass123", Role.CLIENT);
        User saved = User.builder().id(1L).username("newuser")
                .email("new@test.com").password("encoded").role(Role.CLIENT).build();
        UserResponse response = new UserResponse(1L, "newuser", "new@test.com", Role.CLIENT, null);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(response);

        UserResponse result = userService.createUser(request);

        assertThat(result.getUsername()).isEqualTo("newuser");
        verify(passwordEncoder).encode("pass123");
    }

    @Test
    void createUser_throwsWhenUsernameAlreadyTaken() {
        UserRequest request = new UserRequest("existing", "e@test.com", "pass", Role.CLIENT);
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void getUserById_throwsWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteUser_deletesSuccessfully() {
        User user = User.builder().id(1L).username("todelete").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.existsByClientId(1L)).thenReturn(false);

        userService.deleteUser(1L, "admin");

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_throwsWhenDeletingOwnAccount() {
        User user = User.builder().id(1L).username("admin").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deleteUser(1L, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot delete your own account");
    }
}
