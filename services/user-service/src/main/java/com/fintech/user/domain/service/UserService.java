package com.fintech.user.domain.service;

import com.fintech.user.application.dto.CreateUserRequest;
import com.fintech.user.application.dto.UpdateUserRequest;
import com.fintech.user.application.dto.UserResponse;
import com.fintech.user.domain.model.User;
import com.fintech.user.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", maskEmail(request.email()));

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Email already registered: " + maskEmail(request.email()));
        }

        User user = new User(request.email(), request.fullName(), request.phoneNumber());

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException("Email already registered: " + maskEmail(request.email()));
        }

        log.info("User created with ID: {}", user.getId());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        user.updateProfile(request.fullName(), request.phoneNumber());
        user = userRepository.save(user);

        log.info("User updated: {}", userId);
        return UserResponse.from(user);
    }

    @Transactional
    public void deactivateUser(UUID userId) {
        log.info("Deactivating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        user.deactivate();
        userRepository.save(user);

        log.info("User deactivated: {}", userId);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        String maskedLocal = local.charAt(0) + "***";
        String maskedDomain = domain.charAt(0) + "***" + domain.substring(domain.lastIndexOf('.'));
        return maskedLocal + "@" + maskedDomain;
    }

    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String message) {
            super(message);
        }
    }
}
