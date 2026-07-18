package com.ep.auth.service;

import com.ep.auth.config.AppProperties;
import com.ep.auth.domain.B2bClient;
import com.ep.auth.domain.RefreshToken;
import com.ep.auth.domain.UserAccount;
import com.ep.auth.dto.AuthDtos.OnboardUserResponse;
import com.ep.auth.dto.AuthDtos.TokenResponse;
import com.ep.auth.exception.ApiException;
import com.ep.auth.repository.ProfileRepository;
import com.ep.auth.repository.B2bClientRepository;
import com.ep.auth.repository.RefreshTokenRepository;
import com.ep.auth.repository.UserAccountRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final PermissionCacheService permissionCacheService;
    private final JwtService jwtService;
    private final AppProperties properties;
    private final B2bClientRepository b2bClientRepository;
    private final NotificationEmailClient notificationEmailClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserAccountRepository userRepository, ProfileRepository profileRepository,
                       RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                       PasswordGenerator passwordGenerator, PermissionCacheService permissionCacheService,
                       JwtService jwtService, AppProperties properties, B2bClientRepository b2bClientRepository,
                       NotificationEmailClient notificationEmailClient) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.permissionCacheService = permissionCacheService;
        this.jwtService = jwtService;
        this.properties = properties;
        this.b2bClientRepository = b2bClientRepository;
        this.notificationEmailClient = notificationEmailClient;
    }

    @Transactional
    public OnboardUserResponse onboard(UUID tenantId, UUID orgId, String email, String profileName) {
        userRepository.findByTenantAndEmail(tenantId, email).ifPresent(user -> {
            throw new ApiException(HttpStatus.CONFLICT, "USER_EXISTS", "Email already exists for tenant");
        });
        String temporaryPassword = passwordGenerator.temporaryPassword();
        UserAccount user = userRepository.save(new UserAccount(
                tenantId, orgId, email, passwordEncoder.encode(temporaryPassword), true));
        profileRepository.findByTenantAndName(tenantId, profileName)
                .ifPresent(profile -> profileRepository.assignProfileToUser(user.getId(), profile.getId()));
        notificationEmailClient.sendTemporaryPassword(tenantId, orgId, user.getId(), user.getEmail(), temporaryPassword);
        return new OnboardUserResponse(user.getId(), user.getEmail(), temporaryPassword);
    }

    @Transactional
    public TokenResponse login(UUID tenantId, String email, String password) {
        return login(tenantId, null, email, password);
    }

    @Transactional
    public TokenResponse login(UUID tenantId, UUID orgId, String email, String password) {
        UserAccount user = findUser(tenantId, email);
        validateOptionalOrg(user, orgId);
        ensureLoginAllowed(user);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.registerFailedAttempt(properties.security().maxFailedLoginAttempts());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
        }
        if (user.isTemporaryPassword()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PASSWORD_CHANGE_REQUIRED", "Temporary password cannot be used for login");
        }
        user.resetFailedAttempts();
        return issueTokenPair(user, "ep");
    }

    @Transactional
    public void changePassword(UUID tenantId, String email, String temporaryPassword, String newPassword) {
        changePassword(tenantId, null, email, temporaryPassword, newPassword);
    }

    @Transactional
    public void changePassword(UUID tenantId, UUID orgId, String email, String temporaryPassword, String newPassword) {
        UserAccount user = findUser(tenantId, email);
        validateOptionalOrg(user, orgId);
        if (!user.isTemporaryPassword() || !passwordEncoder.matches(temporaryPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TEMPORARY_PASSWORD", "Temporary password is invalid");
        }
        user.setPassword(passwordEncoder.encode(newPassword), false);
        permissionCacheService.evictUser(user.getId());
    }

    @Transactional
    public OnboardUserResponse forgotPassword(UUID tenantId, String email) {
        return forgotPassword(tenantId, null, email);
    }

    @Transactional
    public OnboardUserResponse forgotPassword(UUID tenantId, UUID orgId, String email) {
        UserAccount user = findUser(tenantId, email);
        validateOptionalOrg(user, orgId);
        String temporaryPassword = passwordGenerator.temporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword), true);
        notificationEmailClient.sendTemporaryPassword(tenantId, user.getOrgId(), user.getId(), user.getEmail(), temporaryPassword);
        return new OnboardUserResponse(user.getId(), user.getEmail(), temporaryPassword);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        RefreshToken persisted = refreshTokenRepository.findByTokenHash(jwtService.sha256(refreshToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid"));
        if (persisted.isRevoked() || persisted.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is expired or revoked");
        }
        persisted.revoke();
        UserAccount user = userRepository.findById(persisted.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
        return issueTokenPair(user, "ep");
    }

    @Transactional
    public void invalidate(String refreshToken) {
        refreshTokenRepository.findByTokenHash(jwtService.sha256(refreshToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public TokenResponse generate(UUID userId, String audience) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
        return issueTokenPair(user, audience);
    }

    public TokenResponse generateB2b(String clientId, String clientSecret, String tenantId, String orgId, String audience) {
        B2bClient client = b2bClientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CLIENT", "Invalid client credentials"));
        if (!client.isActive() || client.isExpired(Instant.now())
                || !passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CLIENT", "Invalid client credentials");
        }

        UUID requestedTenantId = parseRequestUuid(tenantId, "tenantId");
        UUID requestedOrgId = parseRequestUuid(orgId, "orgId");
        if (requestedTenantId != null && !Objects.equals(requestedTenantId, client.getTenantId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INVALID_CLIENT_SCOPE", "Client is not authorized for the requested tenant");
        }
        if (requestedOrgId != null && !Objects.equals(requestedOrgId, client.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INVALID_CLIENT_SCOPE", "Client is not authorized for the requested organization");
        }
        if (audience != null && !audience.isBlank() && !Objects.equals(audience, client.getAudience())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INVALID_CLIENT_AUDIENCE", "Client is not authorized for the requested audience");
        }

        JwtService.IssuedAccessToken accessToken = jwtService.issueServiceAccessToken(
                client.getClientId(), client.getTenantId(), client.getOrgId(), client.getPermissions(),
                client.getAudience());
        return new TokenResponse(accessToken.value(), null, accessToken.expiresAt(), List.of("SERVICE_CLIENT"),
                client.getPermissions());
    }

    private TokenResponse issueTokenPair(UserAccount user, String audience) {
        List<String> profiles = permissionCacheService.profilesForUser(user.getId());
        List<String> permissions = permissionCacheService.permissionsForUser(user.getId());
        JwtService.IssuedAccessToken accessToken = jwtService.issueAccessToken(user, profiles, permissions, audience);
        String refreshToken = randomToken();
        refreshTokenRepository.save(new RefreshToken(
                user.getId(),
                jwtService.sha256(refreshToken),
                Instant.now().plusSeconds(properties.jwt().refreshTokenDays() * 24 * 60 * 60)
        ));
        return new TokenResponse(accessToken.value(), refreshToken, accessToken.expiresAt(), profiles, permissions);
    }

    private UserAccount findUser(UUID tenantId, String email) {
        return userRepository.findByTenantAndEmail(tenantId, email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials"));
    }

    private void ensureLoginAllowed(UserAccount user) {
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_INACTIVE", "User is inactive");
        }
        if (user.isLocked()) {
            throw new ApiException(HttpStatus.LOCKED, "USER_LOCKED", "Account is locked");
        }
    }

    private void validateOptionalOrg(UserAccount user, UUID requestedOrgId) {
        if (requestedOrgId != null && !Objects.equals(requestedOrgId, user.getOrgId())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ORG_CONTEXT",
                    "Organization does not match the user account");
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private UUID parseRequestUuid(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_UUID", fieldName + " must be a valid UUID");
        }
    }
}

