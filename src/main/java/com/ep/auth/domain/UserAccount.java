package com.ep.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    private UUID id;
    private UUID tenantId;
    private UUID orgId;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private boolean temporaryPassword = true;
    @Column(nullable = false)
    private int failedAttempts;
    @Column(nullable = false)
    private boolean locked;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected UserAccount() {
    }

    public UserAccount(UUID tenantId, UUID orgId, String email, String passwordHash, boolean temporaryPassword) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.orgId = orgId;
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
        this.temporaryPassword = temporaryPassword;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isTemporaryPassword() {
        return temporaryPassword;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isActive() {
        return active;
    }

    public void setPassword(String passwordHash, boolean temporaryPassword) {
        this.passwordHash = passwordHash;
        this.temporaryPassword = temporaryPassword;
        this.failedAttempts = 0;
        this.locked = false;
        this.updatedAt = Instant.now();
    }

    public void registerFailedAttempt(int lockAt) {
        failedAttempts++;
        if (failedAttempts >= lockAt) {
            locked = true;
        }
        updatedAt = Instant.now();
    }

    public void resetFailedAttempts() {
        failedAttempts = 0;
        updatedAt = Instant.now();
    }
}
