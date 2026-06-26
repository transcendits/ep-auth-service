package com.ep.auth.service;

import com.ep.auth.domain.Permission;
import com.ep.auth.domain.Profile;
import com.ep.auth.dto.AuthDtos.PermissionResponse;
import com.ep.auth.dto.AuthDtos.ProfileResponse;
import com.ep.auth.exception.ApiException;
import com.ep.auth.repository.PermissionRepository;
import com.ep.auth.repository.ProfileRepository;
import com.ep.auth.repository.UserAccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfilePermissionService {
    private final ProfileRepository profileRepository;
    private final PermissionRepository permissionRepository;
    private final UserAccountRepository userAccountRepository;
    private final PermissionCacheService cacheService;

    public ProfilePermissionService(ProfileRepository profileRepository, PermissionRepository permissionRepository,
                                    UserAccountRepository userAccountRepository, PermissionCacheService cacheService) {
        this.profileRepository = profileRepository;
        this.permissionRepository = permissionRepository;
        this.userAccountRepository = userAccountRepository;
        this.cacheService = cacheService;
    }

    public List<ProfileResponse> profiles(UUID tenantId) {
        return profileRepository.findByTenant(tenantId).stream()
                .map(p -> new ProfileResponse(p.getId(), p.getTenantId(), p.getName(), p.getDescription()))
                .toList();
    }

    public List<PermissionResponse> permissions() {
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void assignProfile(UUID userId, UUID profileId) {
        userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
        profileRepository.findById(profileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Profile was not found"));
        profileRepository.assignProfileToUser(userId, profileId);
        cacheService.evictUser(userId);
    }

    private PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getCode(), permission.getDescription());
    }
}
