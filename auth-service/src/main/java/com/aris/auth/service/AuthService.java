package com.aris.auth.service;

import com.aris.auth.domain.UserEntity;
import com.aris.auth.dto.LoginRequest;
import com.aris.auth.dto.MeResponse;
import com.aris.auth.dto.RegisterRequest;
import com.aris.auth.dto.TokenResponse;
import com.aris.auth.repository.UserRepository;
import com.aris.common.security.ArisJwtService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ArisJwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ArisJwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name().trim());
        user.setRole("USER");
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
        return toToken(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return toToken(user);
    }

    @Transactional(readOnly = true)
    public MeResponse me(String userId) {
        UUID id = UUID.fromString(userId);
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new MeResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }

    private TokenResponse toToken(UserEntity user) {
        String token = jwtService.issueToken(
                user.getId().toString(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
        return new TokenResponse(token, "Bearer", jwtService.getExpirySeconds());
    }
}
