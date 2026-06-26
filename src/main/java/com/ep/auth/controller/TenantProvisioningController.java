package com.ep.auth.controller;

import com.ep.auth.dto.AuthDtos.TenantProvisionRequest;
import com.ep.auth.dto.AuthDtos.TenantProvisionResponse;
import com.ep.auth.service.TenantProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantProvisioningController {
    private final TenantProvisioningService provisioningService;

    public TenantProvisioningController(TenantProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @PostMapping("/internal/tenants/provision")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantProvisionResponse provision(@Valid @RequestBody TenantProvisionRequest request) {
        return provisioningService.provision(request);
    }
}
