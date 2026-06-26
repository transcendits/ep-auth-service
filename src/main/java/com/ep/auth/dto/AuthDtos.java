package com.ep.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record OnboardUserRequest(UUID tenantId, UUID orgId, @Email @NotBlank String email, @NotBlank String profileName) {
    }

    public record OnboardUserResponse(UUID userId, String email, String temporaryPassword) {
    }

    public record LoginRequest(UUID tenantId, UUID orgId, @Email @NotBlank String email, @NotBlank String password) {
    }

    public record ChangePasswordRequest(UUID tenantId, UUID orgId, @Email @NotBlank String email,
                                        @NotBlank String temporaryPassword, @NotBlank String newPassword) {
    }

    public record ForgotPasswordRequest(UUID tenantId, UUID orgId, @Email @NotBlank String email) {
    }

    public record TokenResponse(String accessToken, String refreshToken, Instant expiresAt, List<String> profiles,
                                List<String> permissions) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record InvalidateTokenRequest(@NotBlank String refreshToken) {
    }

    public record AssignProfileRequest(@NotNull UUID userId, @NotNull UUID profileId) {
    }

    public record GenerateB2bTokenRequest(@NotBlank String clientId, @NotBlank String clientSecret, String tenantId,
                                          String orgId, String audience) {
    }

    public record GenerateTokenRequest(@NotNull UUID userId, String audience) {
    }

    public record TenantProvisionRequest(@NotNull UUID tenantId, @NotBlank String name, UUID orgId,
                                         @Size(max = 120) String b2bClientName,
                                         @Size(max = 160) String b2bAudience,
                                         List<@NotBlank String> b2bPermissions) {
    }

    public record TenantProvisionResponse(UUID tenantId, String keyId, List<String> profiles,
                                          B2bClientCredentials b2bClient) {
    }

    public record B2bClientCredentials(String clientId, String clientSecret, UUID tenantId, UUID orgId,
                                       String audience, List<String> permissions, Instant expiresAt,
                                       boolean created) {
    }

    public record ProfileResponse(UUID id, UUID tenantId, String name, String description) {
    }

    public record PermissionResponse(UUID id, String code, String description) {
    }
}
