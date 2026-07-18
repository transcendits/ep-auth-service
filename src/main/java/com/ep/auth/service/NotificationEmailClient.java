package com.ep.auth.service;

import com.ep.auth.client.NotificationServiceClient;
import com.ep.auth.client.NotificationServiceClient.NotificationEmailRequest;
import com.ep.auth.client.NotificationServiceClient.Recipient;
import com.ep.auth.config.AppProperties;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationEmailClient {
    private static final Logger log = LoggerFactory.getLogger(NotificationEmailClient.class);
    private static final String TEMP_PASSWORD_TEMPLATE = "AUTH_TEMP_PASSWORD";

    private final NotificationServiceClient notificationServiceClient;
    private final AppProperties properties;

    public NotificationEmailClient(NotificationServiceClient notificationServiceClient, AppProperties properties) {
        this.notificationServiceClient = notificationServiceClient;
        this.properties = properties;
    }

    public void sendTemporaryPassword(UUID tenantId, UUID orgId, UUID userId, String email, String temporaryPassword) {
        AppProperties.Notification notification = properties.notification();
        if (notification == null || !notification.enabled()) {
            log.debug("Temporary password email skipped because notification integration is disabled");
            return;
        }

        try {
            NotificationEmailRequest request = temporaryPasswordRequest(
                    tenantId, orgId, userId, email, temporaryPassword, notification.loginUrl());
            notificationServiceClient.sendEmail(request);
            log.info("Temporary password email queued for userId={}, tenantId={}, orgId={}", userId, tenantId, orgId);
        } catch (Exception ex) {
            log.error("Temporary password email could not be queued for userId={}, tenantId={}, orgId={}: {}",
                    userId, tenantId, orgId, ex.getMessage(), ex);
            log.warn("Temporary password email could not be queued for userId={}, tenantId={}, orgId={}: {}",
                    userId, tenantId, orgId, ex.getMessage());
        }
    }

    private NotificationEmailRequest temporaryPasswordRequest(UUID tenantId, UUID orgId, UUID userId, String email,
                                                              String temporaryPassword, String loginUrl) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName(email),
                "email", email,
                "tempPassword", temporaryPassword,
                "loginUrl", loginUrl
        );
        String idempotencyKey = "auth-temp-password-" + userId + "-" + System.currentTimeMillis();
        return new NotificationEmailRequest(
                tenantId == null ? null : tenantId.toString(),
                orgId == null ? null : orgId.toString(),
                TEMP_PASSWORD_TEMPLATE,
                Map.of(),
                List.of(new Recipient(email, variables, idempotencyKey)),
                UUID.randomUUID().toString(),
                idempotencyKey
        );
    }

    private String firstName(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        String localPart = at > 0 ? email.substring(0, at) : "there";
        int dot = localPart.indexOf('.');
        int underscore = localPart.indexOf('_');
        int separator = minPositive(dot, underscore);
        return separator == Integer.MAX_VALUE ? localPart : localPart.substring(0, separator);
    }

    private int minPositive(int left, int right) {
        int positiveLeft = left > 0 ? left : Integer.MAX_VALUE;
        int positiveRight = right > 0 ? right : Integer.MAX_VALUE;
        return Math.min(positiveLeft, positiveRight);
    }
}
