package com.ep.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile {
    @Id
    private UUID id;
    private UUID tenantId;
    @Column(nullable = false)
    private String name;
    private String description;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Profile() {
    }

    public Profile(UUID tenantId, String name, String description) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
