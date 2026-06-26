package com.ep.auth.service;

import com.ep.auth.repository.PermissionRepository;
import com.ep.auth.repository.ProfileRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class PermissionCacheService {
    private final PermissionRepository permissionRepository;
    private final ProfileRepository profileRepository;

    public PermissionCacheService(PermissionRepository permissionRepository, ProfileRepository profileRepository) {
        this.permissionRepository = permissionRepository;
        this.profileRepository = profileRepository;
    }

    @Cacheable(cacheNames = "userPermissions", key = "#userId")
    public List<String> permissionsForUser(UUID userId) {
        return permissionRepository.findPermissionCodesByUserId(userId);
    }

    @Cacheable(cacheNames = "userProfiles", key = "#userId")
    public List<String> profilesForUser(UUID userId) {
        return profileRepository.findProfileNamesByUserId(userId);
    }

    @CacheEvict(cacheNames = {"userPermissions", "userProfiles"}, key = "#userId")
    public void evictUser(UUID userId) {
    }
}
