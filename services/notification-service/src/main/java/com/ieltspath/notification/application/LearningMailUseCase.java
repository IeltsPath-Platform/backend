package com.ieltspath.notification.application;

import com.ieltspath.events.user.UserEmailVerificationStateV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Learning mails (essay graded, overdue, results, examiner jobs) and the billing notice for a granted plan. Learning mails
 * honour the learner's switch and carry an unsubscribe link; billing mails are always sent. The address comes from the
 * local contact table, so a missing contact skips the mail instead of failing the event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("${notification.mail.enabled:false} and '${spring.mail.host:}' != '' and '${notification.mail.from:}' != '' and '${notification.mail.frontend-base-url:}' != '' and '${notification.mail.unsubscribe-secret:}' != ''")
public class LearningMailUseCase {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final String DEFAULT_NAME = "bạn";

    private final MailTemplateRenderer renderer;
    private final MailSender sender;
    private final MailDeliveryRepository deliveries;
    private final LearningMailRepository repository;
    private final MailUnsubscribeTokens tokens;

    @Value("${notification.mail.frontend-base-url:}") private String frontendBaseUrl;
    @Value("${notification.mail.api-base-url:http://localhost:8080}") private String apiBaseUrl;

    /** Keeps the address and name that later learning and billing mails are sent to. */
    public void recordContact(UserEmailVerificationStateV1 event) {
        if (event == null || event.eventId() == null || event.userId() == null || event.occurredAt() == null
                || isBlank(event.email())) {
            throw new IllegalArgumentException("User contact event is invalid");
        }
        repository.upsertContact(event.userId(), event.email(), event.displayName(), event.occurredAt());
    }

    public void gradingCompleted(UUID eventId, UUID learnerId) {
        learner(MailTemplate.ESSAY_GRADED, eventId, learnerId, "/writing-reviews");
    }

    public void gradingOverdue(UUID eventId, UUID learnerId, UUID gradingJobId) {
        learner(MailTemplate.GRADING_OVERDUE, eventId, learnerId, "/grading-jobs/" + gradingJobId);
    }

    public void resultReady(UUID eventId, UUID userId) {
        learner(MailTemplate.RESULT_READY, eventId, userId, "/overview?tab=detailed-stats");
    }

    /** Billing notice: sent regardless of the learning switch, since it confirms a change to access. */
    public void subscriptionGranted(UUID eventId, UUID userId, Instant endsAt) {
        if (eventId == null || userId == null || endsAt == null) {
            throw new IllegalArgumentException("Subscription granted event is invalid");
        }
        if (deliveries.exists(eventId, MailTemplate.SUBSCRIPTION_GRANTED.key())) return;
        Optional<LearningMailRepository.Contact> contact = repository.findContact(userId);
        if (contact.isEmpty()) {
            log.info("Subscription granted mail skipped: no contact on file");
            return;
        }
        deliver(MailTemplate.SUBSCRIPTION_GRANTED, eventId, contact.get().email(),
                Map.of("displayName", name(contact.get().displayName()), "endsAt", WHEN.format(endsAt)));
    }

    public void examinerDueSoon(UUID eventId, UUID examinerId) {
        examiner(MailTemplate.EXAMINER_DUE_SOON, eventId, examinerId, "/examiner/grading/writing", Map.of());
    }

    /** New jobs are collected and sent as one digest per examiner per hour; see {@link #flushExaminerDigests}. */
    public void examinerNewJob(UUID gradingJobId, UUID examinerId, Instant createdAt) {
        repository.addPendingExaminerJob(gradingJobId, examinerId, createdAt);
    }

    /** Sends one digest for each examiner with waiting jobs, then clears them. Returns how many mails went out. */
    public int flushExaminerDigests(Instant now) {
        int sent = 0;
        Instant hour = now.truncatedTo(ChronoUnit.HOURS);
        for (UUID examinerId : repository.examinersWithPendingJobs()) {
            int jobs = repository.pendingExaminerJobCount(examinerId);
            UUID digestId = UUID.nameUUIDFromBytes(("examiner-digest:" + examinerId + ":" + hour)
                    .getBytes(StandardCharsets.UTF_8));
            // Jobs are cleared even when no mail goes out (no address, or the examiner switched mail off).
            if (examiner(MailTemplate.EXAMINER_NEW_JOBS, digestId, examinerId, "/examiner/grading/writing",
                    Map.of("jobCount", jobs))) {
                sent++;
            }
            repository.clearPendingExaminerJobs(examinerId);
        }
        return sent;
    }

    private void learner(MailTemplate template, UUID eventId, UUID userId, String path) {
        if (eventId == null || userId == null) throw new IllegalArgumentException("Learning mail event is invalid");
        examiner(template, eventId, userId, path, Map.of());
    }

    /**
     * Shared path for learner and examiner learning mails: contact, switch, unsubscribe link, render, send.
     * Returns true only when a mail was sent.
     */
    private boolean examiner(MailTemplate template, UUID eventId, UUID recipientId, String path, Map<String, Object> extra) {
        if (deliveries.exists(eventId, template.key())) return false;
        Optional<LearningMailRepository.Contact> contact = repository.findContact(recipientId);
        if (contact.isEmpty()) {
            log.info("Learning mail skipped template={}: no contact on file", template.key());
            return false;
        }
        if (!repository.learningEnabled(recipientId)) return false;
        Map<String, Object> model = new HashMap<>(extra);
        model.put("displayName", name(contact.get().displayName()));
        model.put("actionLink", frontendBaseUrl.replaceAll("/+$", "") + path);
        model.put("unsubscribeLink", unsubscribeLink(recipientId));
        deliver(template, eventId, contact.get().email(), model);
        return true;
    }

    private void deliver(MailTemplate template, UUID eventId, String email, Map<String, Object> model) {
        RenderedMail mail = renderer.render(template, model);
        sender.send(email, mail);
        deliveries.recordSent(eventId, template.key(), Instant.now());
    }

    private String unsubscribeLink(UUID userId) {
        String token = tokens.issue(userId, "learning", Instant.now());
        return apiBaseUrl.replaceAll("/+$", "") + "/mail/unsubscribe?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private static String name(String displayName) {
        return isBlank(displayName) ? DEFAULT_NAME : displayName;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
