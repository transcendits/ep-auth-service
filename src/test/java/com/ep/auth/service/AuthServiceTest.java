package com.ep.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ep.auth.config.AppProperties;
import com.ep.auth.domain.UserAccount;
import com.ep.auth.exception.ApiException;
import com.ep.auth.repository.ProfileRepository;
import com.ep.auth.repository.B2bClientRepository;
import com.ep.auth.repository.RefreshTokenRepository;
import com.ep.auth.repository.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {
    private final UserAccountRepository userRepository = mock(UserAccountRepository.class);
    private final AuthService authService = new AuthService(
            userRepository,
            mock(ProfileRepository.class),
            mock(RefreshTokenRepository.class),
            new BCryptPasswordEncoder(4),
            new PasswordGenerator(),
            mock(PermissionCacheService.class),
            mock(JwtService.class),
            new AppProperties(new AppProperties.Jwt("test", 15, 30), new AppProperties.Security(5)),
            mock(B2bClientRepository.class)
    );

    @Test
    void loginRejectsTemporaryPasswordEvenWhenPasswordMatches() {
        UUID tenantId = UUID.randomUUID();
        var encoder = new BCryptPasswordEncoder(4);
        UserAccount user = new UserAccount(tenantId, UUID.randomUUID(), "person@example.com", encoder.encode("Temp123!"), true);
        AuthService service = new AuthService(
                userRepository,
                mock(ProfileRepository.class),
                mock(RefreshTokenRepository.class),
                encoder,
                new PasswordGenerator(),
                mock(PermissionCacheService.class),
                mock(JwtService.class),
                new AppProperties(new AppProperties.Jwt("test", 15, 30), new AppProperties.Security(5)),
                mock(B2bClientRepository.class)
        );
        when(userRepository.findByTenantAndEmail(tenantId, "person@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(tenantId, "person@example.com", "Temp123!"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Temporary password");
    }

    @Test
    void failedAttemptsLockAccountAtConfiguredThreshold() {
        UserAccount user = new UserAccount(UUID.randomUUID(), UUID.randomUUID(), "person@example.com", "hash", false);

        for (int i = 0; i < 5; i++) {
            user.registerFailedAttempt(5);
        }

        assertThat(user.isLocked()).isTrue();
        assertThat(user.getFailedAttempts()).isEqualTo(5);
    }
}
