package com.ep.auth.repository;

import com.ep.auth.domain.TenantKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantKeyRepository extends JpaRepository<TenantKey, UUID> {
    Optional<TenantKey> findFirstByTenantIdAndActiveTrueOrderByCreatedAtDesc(UUID tenantId);

    Optional<TenantKey> findByKeyIdAndActiveTrue(String keyId);

    List<TenantKey> findByActiveTrue();
}
