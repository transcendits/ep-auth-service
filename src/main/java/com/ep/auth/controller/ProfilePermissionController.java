package com.ep.auth.controller;

import com.ep.auth.dto.AuthDtos.AssignProfileRequest;
import com.ep.auth.dto.AuthDtos.PermissionResponse;
import com.ep.auth.dto.AuthDtos.ProfileResponse;
import com.ep.auth.security.AuthPrincipal;
import com.ep.auth.service.ProfilePermissionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfilePermissionController {
    private final ProfilePermissionService service;

    public ProfilePermissionController(ProfilePermissionService service) {
        this.service = service;
    }

    @GetMapping("/profiles")
    public List<ProfileResponse> profiles(@AuthenticationPrincipal AuthPrincipal principal,
                                          @RequestParam(required = false) UUID tenantId) {
        UUID effectiveTenantId = principal == null ? tenantId : principal.tenantId();
        return service.profiles(effectiveTenantId);
    }

    @GetMapping("/permissions")
    public List<PermissionResponse> permissions() {
        return service.permissions();
    }

    @PostMapping("/assignprofiletouser")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignProfile(@Valid @RequestBody AssignProfileRequest request) {
        service.assignProfile(request.userId(), request.profileId());
    }
}
