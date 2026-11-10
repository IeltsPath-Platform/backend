package com.ieltspath.notification.application;

import com.ieltspath.events.user.PasswordResetRequestedV1;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
@ConditionalOnExpression("${notification.mail.enabled:false} and '${spring.mail.host:}' != '' and '${notification.mail.from:}' != '' and '${notification.mail.frontend-base-url:}' != ''")
@RequiredArgsConstructor
public class SendPasswordResetMailUseCase {
    private final PasswordResetMailer mailer;
    private final MailDeliveryRepository deliveries;
    @Value("${notification.mail.frontend-base-url:}") private String frontendBaseUrl;

    public void execute(PasswordResetRequestedV1 event) {
        if (event == null || event.eventId() == null || !PasswordResetRequestedV1.EVENT_TYPE.equals(event.eventType())
                || event.userId() == null || event.email() == null || event.email().isBlank()
                || event.resetToken() == null || event.resetToken().isBlank() || event.expiresAt() == null) {
            throw new IllegalArgumentException("Password reset mail event is invalid");
        }
        if (deliveries.exists(event.eventId()) || !event.expiresAt().isAfter(Instant.now())) return;
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.trim().replaceAll("/+$", "");
        if (base.isBlank()) throw new IllegalArgumentException("Password reset frontend URL is not configured");
        String link = base + "/reset-password?token=" + URLEncoder.encode(event.resetToken(), StandardCharsets.UTF_8);
        mailer.send(event.email(), event.displayName(), link);
        deliveries.recordSent(event.eventId(), Instant.now());
    }
}
