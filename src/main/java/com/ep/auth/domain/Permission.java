package com.ep.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String code;
    private String description;

    protected Permission() {
    }

    public Permission(UUID id, String code, String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
