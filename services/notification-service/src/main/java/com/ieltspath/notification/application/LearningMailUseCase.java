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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
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
        RenderedMail mail = renderer.render(MailTemplate.SUBSCRIPTION_GRANTED,
                Map.of("displayName", name(contact.get().displayName()), "endsAt", WHEN.format(endsAt)));
        sender.send(contact.get().email(), mail);
        recordDelivered(MailTemplate.SUBSCRIPTION_GRANTED, eventId);
    }

    public void examinerDueSoon(UUID eventId, UUID examinerId) {
        examiner(MailTemplate.EXAMINER_DUE_SOON, eventId, examinerId, "/examiner/grading/writing", Map.of());
    }

    /** New jobs are collected and sent as one digest per examiner per hour; see {@link #flushExaminerDigests}. */
    public void examinerNewJob(UUID gradingJobId, UUID examinerId, Instant createdAt) {
        repository.addPendingExaminerJob(gradingJobId, examinerId, createdAt);
    }

    /**
     * Sends one digest for each examiner with waiting jobs. The jobs are claimed first, so a job that arrives during the
     * run waits for the next digest. Jobs are dropped when no mail can ever reach the examiner (no address, or the
     * examiner switched learning mail off) and put back when the mail was held back (daily cap, same-hour digest) or
     * could not be sent, so they go out in a later run. Returns how many mails went out.
     */
    public int flushExaminerDigests(Instant now) {
        int sent = 0;
        Instant hour = now.truncatedTo(ChronoUnit.HOURS);
        for (UUID examinerId : repository.examinersWithPendingJobs()) {
            List<UUID> jobs = repository.claimPendingExaminerJobs(examinerId);
            if (jobs.isEmpty()) continue; // another instance claimed them first
            UUID digestId = UUID.nameUUIDFromBytes(("examiner-digest:" + examinerId + ":" + hour)
                    .getBytes(StandardCharsets.UTF_8));
            Delivery outcome;
            try {
                outcome = examiner(MailTemplate.EXAMINER_NEW_JOBS, digestId, examinerId, "/examiner/grading/writing",
                        Map.of("jobCount", jobs.size()));
            } catch (RuntimeException failure) {
                restoreExaminerJobs(examinerId, jobs, now);
                log.warn("Examiner digest failed, jobs kept for the next run errorType={}", failure.getClass().getSimpleName());
                continue;
            }
            if (outcome == Delivery.SENT) {
                sent++;
            } else if (outcome == Delivery.HELD) {
                restoreExaminerJobs(examinerId, jobs, now);
            }
        }
        return sent;
    }

    private void restoreExaminerJobs(UUID examinerId, List<UUID> jobs, Instant now) {
        for (UUID gradingJobId : jobs) {
            repository.addPendingExaminerJob(gradingJobId, examinerId, now);
        }
    }

    /** What became of one learning mail: sent, dropped for good (no address or switched off), or held back to retry later. */
    private enum Delivery { SENT, DROPPED, HELD }

    private void learner(MailTemplate template, UUID eventId, UUID userId, String path) {
        if (eventId == null || userId == null) throw new IllegalArgumentException("Learning mail event is invalid");
        examiner(template, eventId, userId, path, Map.of());
    }

    /**
     * Shared path for learner and examiner learning mails: contact, switch, unsubscribe link, daily cap, render, send.
     */
    private Delivery examiner(MailTemplate template, UUID eventId, UUID recipientId, String path, Map<String, Object> extra) {
        if (deliveries.exists(eventId, template.key())) return Delivery.HELD;
        Optional<LearningMailRepository.Contact> contact = repository.findContact(recipientId);
        if (contact.isEmpty()) {
            log.info("Learning mail skipped template={}: no contact on file", template.key());
            return Delivery.DROPPED;
        }
        if (!repository.learningEnabled(recipientId)) return Delivery.DROPPED;
        Map<String, Object> model = new HashMap<>(extra);
        model.put("displayName", name(contact.get().displayName()));
        model.put("actionLink", frontendBaseUrl.replaceAll("/+$", "") + path);
        model.put("unsubscribeLink", unsubscribeLink(recipientId));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (!deliveries.reserveDailyNonSecuritySlot(recipientId, today, eventId, template.key(), Instant.now())) {
            return Delivery.HELD;
        }
        try {
            RenderedMail mail = renderer.render(template, model);
            sender.send(contact.get().email(), mail);
        } catch (RuntimeException failure) {
            // Only a failure before the send gives the slot back: the mail certainly did not leave.
            deliveries.releaseDailyNonSecuritySlot(recipientId, today, eventId, template.key());
            throw failure;
        }
        recordDelivered(template, eventId);
        return Delivery.SENT;
    }

    /**
     * The mail has already left, so a failure here must not release the daily slot or rethrow: a retry would send it
     * again. Losing this record only costs the duplicate check, which is the smaller harm.
     */
    private void recordDelivered(MailTemplate template, UUID eventId) {
        try {
            deliveries.recordSent(eventId, template.key(), Instant.now());
        } catch (RuntimeException failure) {
            log.warn("Learning mail sent but its record failed template={} errorType={}", template.key(),
                    failure.getClass().getSimpleName());
        }
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
