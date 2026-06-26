CREATE TABLE b2b_clients (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    org_id UUID NULL,
    client_name VARCHAR(120) NOT NULL,
    client_id VARCHAR(160) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(120) NOT NULL,
    audience VARCHAR(160) NOT NULL DEFAULT 'b2b',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX b2b_clients_tenant_default_scope_unique
    ON b2b_clients (tenant_id, client_name)
    WHERE org_id IS NULL;

CREATE UNIQUE INDEX b2b_clients_tenant_org_scope_unique
    ON b2b_clients (tenant_id, org_id, client_name)
    WHERE org_id IS NOT NULL;

CREATE INDEX b2b_clients_tenant_org_idx ON b2b_clients (tenant_id, org_id);

CREATE TABLE b2b_client_permissions (
    b2b_client_id UUID NOT NULL REFERENCES b2b_clients(id) ON DELETE CASCADE,
    permission_code VARCHAR(160) NOT NULL,
    PRIMARY KEY (b2b_client_id, permission_code)
);
