package com.ep.auth.security;

import com.ep.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Claims claims = jwtService.parseAccessToken(header.substring(7));
            UUID userId = UUID.fromString(claims.getSubject());
            UUID tenantId = optionalUuid(claims.get("tenant_id", String.class));
            UUID orgId = optionalUuid(claims.get("org_id", String.class));
            String email = claims.get("email", String.class);
            List<String> profiles = claims.get("profiles", List.class);
            List<String> permissions = claims.get("permissions", List.class);
            AuthPrincipal principal = new AuthPrincipal(userId, tenantId, orgId, email, profiles == null ? List.of() : profiles);
            var authorities = Optional.ofNullable(permissions).orElse(List.of()).stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, "N/A", authorities));
        }
        filterChain.doFilter(request, response);
    }

    private UUID optionalUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
