package com.ep.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Security security, Notification notification) {
    public record Jwt(String issuer, long accessTokenMinutes, long refreshTokenDays) {
    }

    public record Security(int maxFailedLoginAttempts) {
    }

    public record Notification(boolean enabled, String baseUrl, String loginUrl) {
    }
}
