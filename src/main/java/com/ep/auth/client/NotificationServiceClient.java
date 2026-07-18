package com.ep.auth.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ep-notification-service", url = "${NOTIFICATION_SERVICE_URL:http://localhost:8083}")
public interface NotificationServiceClient {

    @PostMapping(value = "/internal/notifications/email/send", consumes = MediaType.APPLICATION_JSON_VALUE)
    BulkEnqueueResponse sendEmail(@RequestBody NotificationEmailRequest request);

    record NotificationEmailRequest(String tenantId, String orgId, String templateKey,
                                    Map<String, Object> globalVariables, List<Recipient> recipients,
                                    String correlationId, String idempotencyKey) {
    }

    record Recipient(String target, Map<String, Object> variables, String idempotencyKey) {
    }

    record BulkEnqueueResponse(List<UUID> messageIds, String status) {
    }
}
