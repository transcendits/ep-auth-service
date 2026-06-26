package com.ep.auth.repository;

import com.ep.auth.domain.B2bClient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface B2bClientRepository extends JpaRepository<B2bClient, UUID> {
    Optional<B2bClient> findByClientId(String clientId);

    Optional<B2bClient> findByTenantIdAndOrgIdAndClientName(UUID tenantId, UUID orgId, String clientName);
}
