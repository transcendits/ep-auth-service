package com.ep.auth.security;

import java.util.List;
import java.util.UUID;

public record AuthPrincipal(UUID userId, UUID tenantId, UUID orgId, String email, List<String> profiles) {
}
