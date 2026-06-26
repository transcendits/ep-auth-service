package com.ep.auth.service;

import com.ep.auth.domain.B2bClient;
import com.ep.auth.domain.Permission;
import com.ep.auth.domain.Profile;
import com.ep.auth.domain.Tenant;
import com.ep.auth.domain.TenantKey;
import com.ep.auth.dto.AuthDtos.B2bClientCredentials;
import com.ep.auth.dto.AuthDtos.TenantProvisionRequest;
import com.ep.auth.dto.AuthDtos.TenantProvisionResponse;
import com.ep.auth.repository.B2bClientRepository;
import com.ep.auth.repository.PermissionRepository;
import com.ep.auth.repository.ProfileRepository;
import com.ep.auth.repository.TenantKeyRepository;
import com.ep.auth.repository.TenantRepository;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TenantProvisioningService {
    public static final List<String> DEFAULT_PROFILES = List.of("SUPER_ADMIN", "HR_ADMIN", "ACCOUNTS_ADMIN", "EMPLOYEE");
    public static final List<String> DEFAULT_B2B_PERMISSIONS = List.of("member:read", "member:write", "member:admin");
    private static final Map<String, String> EMPLOYEE_MANAGEMENT_PERMISSIONS = new LinkedHashMap<>();
    private static final String DEFAULT_CLIENT_NAME = "default-service";
    private static final String DEFAULT_AUDIENCE = "b2b";

    static {
        EMPLOYEE_MANAGEMENT_PERMISSIONS.put("member:admin", "Administer employee management service");
        EMPLOYEE_MANAGEMENT_PERMISSIONS.put("member:write", "Create and update employee management service data");
        EMPLOYEE_MANAGEMENT_PERMISSIONS.put("member:read", "Read employee management service data");
    }

    private final TenantRepository tenantRepository;
    private final TenantKeyRepository tenantKeyRepository;
    private final ProfileRepository profileRepository;
    private final PermissionRepository permissionRepository;
    private final B2bClientRepository b2bClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public TenantProvisioningService(TenantRepository tenantRepository, TenantKeyRepository tenantKeyRepository,
                                     ProfileRepository profileRepository, PermissionRepository permissionRepository,
                                     B2bClientRepository b2bClientRepository, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.tenantKeyRepository = tenantKeyRepository;
        this.profileRepository = profileRepository;
        this.permissionRepository = permissionRepository;
        this.b2bClientRepository = b2bClientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TenantProvisionResponse provision(TenantProvisionRequest request) {
        UUID tenantId = request.tenantId();
        tenantRepository.findById(tenantId).orElseGet(() -> tenantRepository.save(new Tenant(tenantId, request.name())));
        TenantKey tenantKey = tenantKeyRepository.findFirstByTenantIdAndActiveTrueOrderByCreatedAtDesc(tenantId)
                .orElseGet(() -> createTenantKey(tenantId));
        seedEmployeeManagementPermissions();

        for (String profileName : DEFAULT_PROFILES) {
            Profile profile = profileRepository.findByTenantAndName(tenantId, profileName)
                    .orElseGet(() -> profileRepository.save(new Profile(tenantId, profileName, profileName + " default profile")));
            assignDefaults(profileName, profile.getId());
        }

        B2bClientCredentials credentials = provisionB2bClient(request);
        return new TenantProvisionResponse(tenantId, tenantKey.getKeyId(), DEFAULT_PROFILES, credentials);
    }

    private TenantKey createTenantKey(UUID tenantId) {
        String keyId = "tenant-" + tenantId + "-" + UUID.randomUUID();
        KeyPair keyPair = PemKeyUtils.generateRsaKeyPair();
        return tenantKeyRepository.save(new TenantKey(
                tenantId,
                keyId,
                PemKeyUtils.publicPem(keyPair.getPublic()),
                PemKeyUtils.privatePem(keyPair.getPrivate())
        ));
    }

    private B2bClientCredentials provisionB2bClient(TenantProvisionRequest request) {
        String clientName = StringUtils.hasText(request.b2bClientName())
                ? request.b2bClientName().trim() : DEFAULT_CLIENT_NAME;
        String audience = StringUtils.hasText(request.b2bAudience())
                ? request.b2bAudience().trim() : DEFAULT_AUDIENCE;
        List<String> permissions = DEFAULT_B2B_PERMISSIONS;

        return b2bClientRepository.findByTenantIdAndOrgIdAndClientName(request.tenantId(), request.orgId(), clientName)
                .map(existing -> new B2bClientCredentials(
                        existing.getClientId(), null, existing.getTenantId(), existing.getOrgId(),
                        existing.getAudience(), existing.getPermissions(), existing.getExpiresAt(), false))
                .orElseGet(() -> createB2bClient(request.tenantId(), request.orgId(), clientName, audience, permissions));
    }

    private B2bClientCredentials createB2bClient(UUID tenantId, UUID orgId, String clientName, String audience,
                                                  List<String> permissions) {
        String clientId = "ep-" + tenantId.toString().substring(0, 8) + "-"
                + (orgId == null ? "tenant" : orgId.toString().substring(0, 8)) + "-"
                + randomUrlToken(12);
        String clientSecret = randomUrlToken(48);
        Instant expiresAt = Instant.now().plus(365, ChronoUnit.DAYS);
        B2bClient client = b2bClientRepository.save(new B2bClient(
                tenantId, orgId, clientName, clientId, passwordEncoder.encode(clientSecret), audience,
                permissions, expiresAt));
        return new B2bClientCredentials(client.getClientId(), clientSecret, tenantId, orgId, audience,
                client.getPermissions(), expiresAt, true);
    }

    private String randomUrlToken(int bytes) {
        byte[] value = new byte[bytes];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private void assignDefaults(String profileName, UUID profileId) {
        List<String> permissions = switch (profileName) {
            case "SUPER_ADMIN" -> List.of("tenant:provision", "user:onboard", "user:read", "profile:manage",
                    "permission:read", "token:generate", "member:admin", "member:write", "member:read");
            case "HR_ADMIN" -> List.of("user:onboard", "user:read", "profile:manage", "permission:read",
                    "member:admin", "member:write", "member:read");
            case "ACCOUNTS_ADMIN" -> List.of("user:read", "permission:read", "member:read");
            default -> List.of("member:read");
        };
        for (String code : permissions) {
            permissionRepository.findByCode(code)
                    .ifPresent(permission -> profileRepository.assignPermissionToProfile(profileId, permission.getId()));
        }
    }

    private void seedEmployeeManagementPermissions() {
        EMPLOYEE_MANAGEMENT_PERMISSIONS.forEach((code, description) ->
                permissionRepository.findByCode(code)
                        .orElseGet(() -> permissionRepository.save(
                                new Permission(UUID.nameUUIDFromBytes(code.getBytes(StandardCharsets.UTF_8)),
                                        code, description))));
    }
}
