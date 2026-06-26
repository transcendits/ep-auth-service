package com.ep.auth.service;

import com.ep.auth.config.AppProperties;
import com.ep.auth.domain.TenantKey;
import com.ep.auth.domain.UserAccount;
import com.ep.auth.exception.ApiException;
import com.ep.auth.repository.TenantKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final TenantKeyRepository tenantKeyRepository;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(TenantKeyRepository tenantKeyRepository, AppProperties properties, ObjectMapper objectMapper) {
        this.tenantKeyRepository = tenantKeyRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public IssuedAccessToken issueAccessToken(UserAccount user, List<String> profiles, List<String> permissions, String audience) {
        TenantKey key = tenantKeyRepository.findFirstByTenantIdAndActiveTrueOrderByCreatedAtDesc(user.getTenantId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "TENANT_KEY_MISSING", "No active RSA key exists"));
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.jwt().accessTokenMinutes() * 60);
        PrivateKey privateKey = PemKeyUtils.parsePrivate(key.getPrivateKeyPem());
        String jwt = Jwts.builder()
                .header().keyId(key.getKeyId()).and()
                .issuer(properties.jwt().issuer())
                .audience().add(audience == null || audience.isBlank() ? "ep" : audience).and()
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("tenant_id", user.getTenantId() == null ? null : user.getTenantId().toString())
                .claim("org_id", user.getOrgId() == null ? null : user.getOrgId().toString())
                .claim("email", user.getEmail())
                .claim("profiles", profiles)
                .claim("permissions", permissions)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
        return new IssuedAccessToken(jwt, expiresAt);
    }

    public IssuedAccessToken issueServiceAccessToken(String clientId, UUID tenantId, UUID orgId, List<String> permissions,
                                                     String audience) {
        TenantKey key = tenantKeyRepository.findFirstByTenantIdAndActiveTrueOrderByCreatedAtDesc(tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "TENANT_KEY_MISSING", "No active RSA key exists"));
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.jwt().accessTokenMinutes() * 60);
        PrivateKey privateKey = PemKeyUtils.parsePrivate(key.getPrivateKeyPem());
        String jwt = Jwts.builder()
                .header().keyId(key.getKeyId()).and()
                .issuer(properties.jwt().issuer())
                .audience().add(audience == null || audience.isBlank() ? "b2b" : audience).and()
                .subject(clientId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("token_type", "service")
                .claim("client_id", clientId)
                .claim("tenant_id", tenantId == null ? null : tenantId.toString())
                .claim("org_id", orgId == null ? null : orgId.toString())
                .claim("profiles", List.of("SERVICE_CLIENT"))
                .claim("permissions", permissions)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
        return new IssuedAccessToken(jwt, expiresAt);
    }

    public Claims parseAccessToken(String token) {
        String keyId = readKeyId(token);
        TenantKey key = tenantKeyRepository.findByKeyIdAndActiveTrue(keyId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Unknown signing key"));
        return Jwts.parser()
                .requireIssuer(properties.jwt().issuer())
                .verifyWith(PemKeyUtils.parsePublic(key.getPublicKeyPem()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String readKeyId(String token) {
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
            return objectMapper.readTree(headerJson).path("kid").asText();
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Malformed token");
        }
    }

    public Map<String, Object> jwks() {
        List<Map<String, Object>> keys = tenantKeyRepository.findByActiveTrue().stream()
                .map(key -> {
                    RSAPublicKey publicKey = PemKeyUtils.parsePublic(key.getPublicKeyPem());
                    return Map.<String, Object>of(
                            "kty", "RSA",
                            "use", "sig",
                            "kid", key.getKeyId(),
                            "alg", "RS256",
                            "n", base64Url(publicKey.getModulus()),
                            "e", base64Url(publicKey.getPublicExponent())
                    );
                })
                .toList();
        return Map.of("keys", keys);
    }

    public String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record IssuedAccessToken(String value, Instant expiresAt) {
    }
}
