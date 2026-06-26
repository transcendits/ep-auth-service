# ep-auth-service

Enterprise authentication and authorization microservice implemented from the agreed baseline design.

## Stack

- Java 21
- Spring Boot 3.5.x
- PostgreSQL
- Redis
- Flyway
- RS256 JWT with tenant-scoped RSA keys
- Docker and Kubernetes manifests

## Run locally

```bash
docker compose -f docker/docker-compose.yml up -d
mvn spring-boot:run
```

The service listens on `http://localhost:8080`.

If startup fails with `FATAL: role "ep_auth" does not exist`, your local PostgreSQL instance does not have the application role yet, or an old Docker volume was created before the init script existed.

For Docker, reset the local database volume and start it again:

```bash
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d
```

When running from IntelliJ with `DB_USERNAME=postgres` and `DB_PASSWORD=postgres`, set the active Spring profile to:

```text
local
```

or add this VM option:

```text
-Dspring.profiles.active=local
```

When running from IntelliJ against the bundled Docker database, you can also use:

```text
docker
```

or add this VM option:

```text
-Dspring.profiles.active=docker
```

For an already installed local PostgreSQL server, run this as a superuser:

```bash
psql -U postgres -f docs/local-postgres-setup.sql
```

## Main APIs

- `POST /internal/tenants/provision`
- `POST /auth/user/onboard`
- `POST /auth/user/login`
- `POST /auth/user/changepassword`
- `POST /auth/user/forgotpassword`
- `POST /auth/token/refresh`
- `POST /auth/token/invalidate`
- `GET /auth/.well-known/jwks.json`
- `GET /profiles`
- `GET /permissions`
- `POST /assignprofiletouser`
- `POST /generateb2b`
- `POST /generateb2c`

## Security model

Tenant and org context are read from JWT claims. Platform users have `tenant_id` and `org_id` as `NULL`.
Temporary passwords are BCrypt hashed but cannot be used for login; users must call `changepassword` first.
Accounts lock after five failed login attempts.
# Tenant B2B client provisioning

`POST /internal/tenants/provision` provisions the tenant key, default profiles, and one database-backed B2B client for the requested tenant and optional organization.

```json
{
  "tenantId": "11111111-1111-1111-1111-111111111111",
  "name": "Example Tenant",
  "orgId": "22222222-2222-2222-2222-222222222222",
  "b2bClientName": "employee-service",
  "b2bAudience": "b2b",
  "b2bPermissions": ["member:read", "member:write", "member:admin"]
}
```

The response includes `b2bClient.clientSecret` only when `b2bClient.created` is `true`. Store it immediately in the platform secret manager. Repeating the same tenant/org/client-name request is idempotent and returns the existing client ID with a null secret.

Use the issued credentials with `POST /auth/token/generateb2b`:

```json
{
  "clientId": "<provisioned-client-id>",
  "clientSecret": "<one-time-secret>",
  "tenantId": "11111111-1111-1111-1111-111111111111",
  "orgId": "22222222-2222-2222-2222-222222222222",
  "audience": "b2b"
}
```
# ep-auth-service
# ep-auth-service
# ep-auth-service
# ep-auth-service
