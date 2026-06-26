package com.ep.auth.controller;

import com.ep.auth.dto.AuthDtos.ChangePasswordRequest;
import com.ep.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.ep.auth.dto.AuthDtos.GenerateB2bTokenRequest;
import com.ep.auth.dto.AuthDtos.GenerateTokenRequest;
import com.ep.auth.dto.AuthDtos.InvalidateTokenRequest;
import com.ep.auth.dto.AuthDtos.LoginRequest;
import com.ep.auth.dto.AuthDtos.OnboardUserRequest;
import com.ep.auth.dto.AuthDtos.OnboardUserResponse;
import com.ep.auth.dto.AuthDtos.RefreshRequest;
import com.ep.auth.dto.AuthDtos.TokenResponse;
import com.ep.auth.security.AuthPrincipal;
import com.ep.auth.service.AuthService;
import com.ep.auth.service.JwtService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/user/onboard")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardUserResponse onboard(@AuthenticationPrincipal AuthPrincipal principal,
                                       @Valid @RequestBody OnboardUserRequest request) {
        UUID tenantId = principal == null ? request.tenantId() : principal.tenantId();
        UUID orgId = principal == null ? request.orgId() : principal.orgId();
        return authService.onboard(tenantId, orgId, request.email(), request.profileName());
    }

    @PostMapping("/auth/user/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.tenantId(), request.orgId(), request.email(), request.password());
    }

    @PostMapping("/auth/user/changepassword")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.tenantId(), request.orgId(), request.email(), request.temporaryPassword(),
                request.newPassword());
    }

    @PostMapping("/auth/user/forgotpassword")
    public OnboardUserResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request.tenantId(), request.orgId(), request.email());
    }

    @PostMapping("/auth/token/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/auth/token/invalidate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invalidate(@Valid @RequestBody InvalidateTokenRequest request) {
        authService.invalidate(request.refreshToken());
    }

    @PostMapping({"/generateb2b", "/auth/token/generateb2b", "/api/v1/auth/token/generateb2b"})
    public TokenResponse generateB2b(@Valid @RequestBody GenerateB2bTokenRequest request) {
        return authService.generateB2b(request.clientId(), request.clientSecret(), request.tenantId(), request.orgId(),
                request.audience() == null ? "b2b" : request.audience());
    }

    @PostMapping({"/generateb2c", "/auth/token/generateb2c", "/api/v1/auth/token/generateb2c"})
    public TokenResponse generateB2c(@Valid @RequestBody GenerateTokenRequest request) {
        return authService.generate(request.userId(), request.audience() == null ? "b2c" : request.audience());
    }

    @GetMapping("/auth/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwtService.jwks();
    }
}
