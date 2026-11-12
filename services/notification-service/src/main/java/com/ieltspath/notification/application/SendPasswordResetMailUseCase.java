package com.ieltspath.notification.application;

import com.ieltspath.events.user.PasswordResetRequestedV1;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@ConditionalOnExpression("${notification.mail.enabled:false} and '${spring.mail.host:}' != '' and '${notification.mail.from:}' != '' and '${notification.mail.frontend-base-url:}' != ''")
@RequiredArgsConstructor
public class SendPasswordResetMailUseCase {
    private static final String DEFAULT_NAME = "bạn";

    private final MailTemplateRenderer renderer;
    private final MailSender sender;
    private final MailDeliveryRepository deliveries;

    @Value("${notification.mail.frontend-base-url:}") private String frontendBaseUrl;
    @Value("${notification.mail.templates.password-reset.enabled:true}") private boolean enabled;

    public void execute(PasswordResetRequestedV1 event) {
        if (event == null || event.eventId() == null || !PasswordResetRequestedV1.EVENT_TYPE.equals(event.eventType())
                || event.userId() == null || event.email() == null || event.email().isBlank()
                || event.resetToken() == null || event.resetToken().isBlank() || event.expiresAt() == null) {
            throw new IllegalArgumentException("Password reset mail event is invalid");
        }
        if (!enabled || !event.expiresAt().isAfter(Instant.now())) return;
        String templateKey = MailTemplate.PASSWORD_RESET.key();
        if (deliveries.exists(event.eventId(), templateKey)) return;

        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.trim().replaceAll("/+$", "");
        if (base.isBlank()) throw new IllegalArgumentException("Password reset frontend URL is not configured");
        String link = base + "/reset-password?token=" + URLEncoder.encode(event.resetToken(), StandardCharsets.UTF_8);

        String name = event.displayName() == null || event.displayName().isBlank() ? DEFAULT_NAME : event.displayName();
        long minutes = Math.max(1, (Duration.between(Instant.now(), event.expiresAt()).getSeconds() + 59) / 60);
        RenderedMail mail = renderer.render(MailTemplate.PASSWORD_RESET,
                Map.of("displayName", name, "resetLink", link, "expiresInMinutes", minutes));
        sender.send(event.email(), mail);
        deliveries.recordSent(event.eventId(), templateKey, Instant.now());
    }
}
