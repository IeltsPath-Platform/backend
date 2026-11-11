package com.ieltspath.notification.application;

import com.ieltspath.events.access.SubscriptionExpiryNoticeV1;
import com.ieltspath.events.user.EmailVerificationRequestedV1;
import com.ieltspath.events.user.GoogleAccountLinkedV1;
import com.ieltspath.events.user.PasswordChangedV1;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Account-security notices that are always sent (they are not subject to learner preferences). The recipient address
 * comes from the event, so this use case never calls another service.
 */
@Service
@ConditionalOnExpression("${notification.mail.enabled:false} and '${spring.mail.host:}' != '' and '${notification.mail.from:}' != '' and '${notification.mail.frontend-base-url:}' != ''")
@RequiredArgsConstructor
public class SendAccountNoticeMailUseCase {
    private static final String DEFAULT_NAME = "bạn";
    private static final DateTimeFormatter CHANGED_AT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final MailTemplateRenderer renderer;
    private final MailSender sender;
    private final MailDeliveryRepository deliveries;

    @Value("${notification.mail.templates.password-changed.enabled:true}") private boolean passwordChangedEnabled;
    @Value("${notification.mail.templates.google-account-linked.enabled:true}") private boolean googleAccountLinkedEnabled;
    @Value("${notification.mail.templates.email-verification.enabled:true}") private boolean emailVerificationEnabled;
    @Value("${notification.mail.frontend-base-url:}") private String frontendBaseUrl;
    @Value("${notification.mail.templates.subscription-expiring.enabled:true}") private boolean subscriptionExpiringEnabled;
    @Value("${notification.mail.templates.subscription-expired.enabled:true}") private boolean subscriptionExpiredEnabled;

    public void passwordChanged(PasswordChangedV1 event) {
        if (event == null || event.eventId() == null || !PasswordChangedV1.EVENT_TYPE.equals(event.eventType())
                || event.userId() == null || isBlank(event.email()) || event.occurredAt() == null) {
            throw new IllegalArgumentException("Password changed event is invalid");
        }
        if (!passwordChangedEnabled) return;
        send(MailTemplate.PASSWORD_CHANGED, event.eventId(), event.email(), Map.of(
                "displayName", name(event.displayName()),
                "changedAt", CHANGED_AT.format(event.occurredAt())));
    }

    public void googleAccountLinked(GoogleAccountLinkedV1 event) {
        if (event == null || event.eventId() == null || !GoogleAccountLinkedV1.EVENT_TYPE.equals(event.eventType())
                || event.userId() == null || isBlank(event.email()) || event.occurredAt() == null) {
            throw new IllegalArgumentException("Google account linked event is invalid");
        }
        if (!googleAccountLinkedEnabled) return;
        send(MailTemplate.GOOGLE_ACCOUNT_LINKED, event.eventId(), event.email(), Map.of(
                "displayName", name(event.displayName())));
    }

    /** The verification link expires with its token; the minutes left are shown rather than a fixed number. */
    public void emailVerificationRequested(EmailVerificationRequestedV1 event) {
        if (event == null || event.eventId() == null || !EmailVerificationRequestedV1.EVENT_TYPE.equals(event.eventType())
                || event.userId() == null || isBlank(event.email()) || isBlank(event.verificationToken())
                || event.expiresAt() == null) {
            throw new IllegalArgumentException("Email verification event is invalid");
        }
        if (!emailVerificationEnabled || !event.expiresAt().isAfter(Instant.now())) return;
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.trim().replaceAll("/+$", "");
        if (base.isBlank()) throw new IllegalArgumentException("Email verification frontend URL is not configured");
        String link = base + "/verify-email?token=" + URLEncoder.encode(event.verificationToken(), StandardCharsets.UTF_8);
        long minutes = Math.max(1, (Duration.between(Instant.now(), event.expiresAt()).getSeconds() + 59) / 60);
        send(MailTemplate.EMAIL_VERIFICATION, event.eventId(), event.email(), Map.of(
                "displayName", name(event.displayName()),
                "verifyLink", link,
                "expiresInMinutes", minutes));
    }

    /** Premium expiry: "about to end" for the 7-day and 1-day stages, "has ended" for the final stage. */
    public void subscriptionExpiry(SubscriptionExpiryNoticeV1 event) {
        if (event == null || event.eventId() == null || !SubscriptionExpiryNoticeV1.EVENT_TYPE.equals(event.eventType())
                || event.userId() == null || isBlank(event.email()) || event.endsAt() == null) {
            throw new IllegalArgumentException("Subscription expiry notice is invalid");
        }
        boolean ended = "EXPIRED".equals(event.stage());
        boolean aboutToEnd = "SEVEN_DAYS".equals(event.stage()) || "ONE_DAY".equals(event.stage());
        if (!ended && !aboutToEnd) throw new IllegalArgumentException("Unknown subscription expiry stage");
        MailTemplate template = ended ? MailTemplate.SUBSCRIPTION_EXPIRED : MailTemplate.SUBSCRIPTION_EXPIRING;
        boolean enabled = ended ? subscriptionExpiredEnabled : subscriptionExpiringEnabled;
        if (!enabled) return;
        send(template, event.eventId(), event.email(), Map.of(
                "displayName", name(event.displayName()),
                "endsAt", CHANGED_AT.format(event.endsAt())));
    }

    private void send(MailTemplate template, UUID eventId, String email, Map<String, Object> model) {
        if (deliveries.exists(eventId, template.key())) return;
        RenderedMail mail = renderer.render(template, model);
        sender.send(email, mail);
        deliveries.recordSent(eventId, template.key(), Instant.now());
    }

    private static String name(String displayName) {
        return isBlank(displayName) ? DEFAULT_NAME : displayName;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
