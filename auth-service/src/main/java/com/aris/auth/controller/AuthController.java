package com.aris.auth.controller;

import com.aris.auth.dto.LoginRequest;
import com.aris.auth.dto.MeResponse;
import com.aris.auth.dto.RegisterRequest;
import com.aris.auth.dto.TokenResponse;
import com.aris.auth.service.AuthService;
import com.aris.common.security.ArisJwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString(ArisJwtService.CLAIM_USER_ID);
        if (userId == null || userId.isBlank()) {
            userId = jwt.getSubject();
        }
        return authService.me(userId);
    }
}
