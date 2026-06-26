CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tenant_keys (
    id UUID PRIMARY KEY,
    tenant_id UUID NULL REFERENCES tenants(id),
    key_id VARCHAR(120) NOT NULL UNIQUE,
    public_key_pem TEXT NOT NULL,
    private_key_pem TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    tenant_id UUID NULL REFERENCES tenants(id),
    org_id UUID NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    temporary_password BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT users_email_per_tenant UNIQUE (tenant_id, email)
);

CREATE UNIQUE INDEX users_platform_email_unique ON users (email) WHERE tenant_id IS NULL;

CREATE TABLE profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NULL REFERENCES tenants(id),
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT profiles_name_per_tenant UNIQUE (tenant_id, name)
);

CREATE UNIQUE INDEX profiles_platform_name_unique ON profiles (name) WHERE tenant_id IS NULL;

CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(160) NOT NULL UNIQUE,
    description VARCHAR(500)
);

CREATE TABLE profile_permissions (
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (profile_id, permission_id)
);

CREATE TABLE user_profiles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, profile_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(120) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO permissions (id, code, description) VALUES
('00000000-0000-0000-0000-000000000001', 'tenant:provision', 'Provision tenants'),
('00000000-0000-0000-0000-000000000002', 'user:onboard', 'Onboard users'),
('00000000-0000-0000-0000-000000000003', 'user:read', 'Read users'),
('00000000-0000-0000-0000-000000000004', 'profile:manage', 'Manage profiles'),
('00000000-0000-0000-0000-000000000005', 'permission:read', 'Read permissions'),
('00000000-0000-0000-0000-000000000006', 'token:generate', 'Generate B2B/B2C tokens'),
('00000000-0000-0000-0000-000000000007', 'employee:self', 'Employee self-service');
