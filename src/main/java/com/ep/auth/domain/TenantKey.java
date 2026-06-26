package com.ep.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_keys")
public class TenantKey {
    @Id
    private UUID id;
    private UUID tenantId;
    @Column(nullable = false, unique = true)
    private String keyId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKeyPem;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String privateKeyPem;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected TenantKey() {
    }

    public TenantKey(UUID tenantId, String keyId, String publicKeyPem, String privateKeyPem) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.keyId = keyId;
        this.publicKeyPem = publicKeyPem;
        this.privateKeyPem = privateKeyPem;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public boolean isActive() {
        return active;
    }
}
