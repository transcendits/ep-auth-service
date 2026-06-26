package com.ep.auth.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "b2b_clients")
public class B2bClient {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID tenantId;
    private UUID orgId;
    @Column(nullable = false, length = 120)
    private String clientName;
    @Column(nullable = false, unique = true, length = 160)
    private String clientId;
    @Column(nullable = false, length = 120)
    private String clientSecretHash;
    @Column(nullable = false, length = 160)
    private String audience;
    @Column(nullable = false)
    private boolean active = true;
    private Instant expiresAt;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "b2b_client_permissions", joinColumns = @JoinColumn(name = "b2b_client_id"))
    @Column(name = "permission_code", nullable = false, length = 160)
    private Set<String> permissions = new LinkedHashSet<>();

    protected B2bClient() {
    }

    public B2bClient(UUID tenantId, UUID orgId, String clientName, String clientId, String clientSecretHash,
                     String audience, List<String> permissions, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.orgId = orgId;
        this.clientName = clientName;
        this.clientId = clientId;
        this.clientSecretHash = clientSecretHash;
        this.audience = audience;
        this.permissions = new LinkedHashSet<>(permissions);
        this.expiresAt = expiresAt;
    }

    public UUID getTenantId() { return tenantId; }
    public UUID getOrgId() { return orgId; }
    public String getClientName() { return clientName; }
    public String getClientId() { return clientId; }
    public String getClientSecretHash() { return clientSecretHash; }
    public String getAudience() { return audience; }
    public boolean isActive() { return active; }
    public Instant getExpiresAt() { return expiresAt; }
    public List<String> getPermissions() { return List.copyOf(permissions); }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
