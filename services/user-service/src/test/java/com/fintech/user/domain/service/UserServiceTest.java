package com.fintech.user.domain.service;

import com.fintech.user.application.dto.CreateUserRequest;
import com.fintech.user.application.dto.UpdateUserRequest;
import com.fintech.user.application.dto.UserResponse;
import com.fintech.user.domain.model.User;
import com.fintech.user.domain.model.UserStatus;
import com.fintech.user.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("should create user successfully when email is unique")
        void shouldCreateUserSuccessfully() {
            CreateUserRequest request = new CreateUserRequest(
                    "john@example.com", "John Doe", "+1234567890");

            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                return user;
            });

            UserResponse response = userService.createUser(request);

            assertThat(response.email()).isEqualTo("john@example.com");
            assertThat(response.fullName()).isEqualTo("John Doe");
            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw DuplicateEmailException when email already exists")
        void shouldThrowWhenEmailDuplicate() {
            CreateUserRequest request = new CreateUserRequest(
                    "john@example.com", "John Doe", null);

            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(UserService.DuplicateEmailException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUser() {
            UUID userId = UUID.randomUUID();
            User user = new User("john@example.com", "John Doe", null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserResponse response = userService.getUserById(userId);

            assertThat(response.email()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when user not found")
        void shouldThrowWhenNotFound() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(userId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("deactivateUser")
    class DeactivateUser {

        @Test
        @DisplayName("should deactivate existing user")
        void shouldDeactivateUser() {
            UUID userId = UUID.randomUUID();
            User user = new User("john@example.com", "John Doe", null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.deactivateUser(userId);

            assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
            verify(userRepository).save(user);
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("should update user profile fields")
        void shouldUpdateUser() {
            UUID userId = UUID.randomUUID();
            User user = new User("john@example.com", "John Doe", null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            UpdateUserRequest request = new UpdateUserRequest("Jane Doe", "+9876543210");
            UserResponse response = userService.updateUser(userId, request);

            assertThat(user.getFullName()).isEqualTo("Jane Doe");
            assertThat(user.getPhoneNumber()).isEqualTo("+9876543210");
        }
    }
}
