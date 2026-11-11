package com.ieltspath.access.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.domain.entity.OutboxEvent;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
import com.ieltspath.access.domain.repository.SubscriptionExpiryNoticeRepository;
import com.ieltspath.access.domain.repository.SubscriptionExpiryNoticeRepository.DueSubscription;
import com.ieltspath.access.domain.repository.UserEmailVerificationRepository;
import com.ieltspath.access.domain.repository.UserEmailVerificationRepository.Contact;
import com.ieltspath.events.access.SubscriptionExpiryNoticeV1;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Queues the Premium expiry notices. Each stage is claimed before it is queued, so it goes out once. Only the most urgent
 * due stage is sent; the earlier stages that also fell due are marked as claimed without a mail, so a short subscription
 * does not produce a 7-day and a 1-day notice at the same time. Access rules are not changed: a subscription stays
 * ACTIVE and expiry is still derived from its end time.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionExpiryNoticeUseCase {
    static final String SEVEN_DAYS = "SEVEN_DAYS";
    static final String ONE_DAY = "ONE_DAY";
    static final String EXPIRED = "EXPIRED";
    static final String AGGREGATE_TYPE = "Subscription";
    static final String OUTBOX_EVENT_TYPE = "SubscriptionExpiryNotice";

    private static final Duration WEEK = Duration.ofDays(7);
    private static final Duration DAY = Duration.ofDays(1);
    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryNoticeUseCase.class);

    private final SubscriptionExpiryNoticeRepository notices;
    private final UserEmailVerificationRepository contacts;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /** Returns how many notices were queued. Subscriptions without a known address are skipped and retried later. */
    @Transactional
    public int scan(Instant now) {
        int queued = 0;
        int withoutContact = 0;
        for (DueSubscription due : notices.findActiveEndingBefore(now.plus(WEEK))) {
            Optional<Contact> contact = contacts.findContact(due.userId());
            if (contact.isEmpty()) {
                withoutContact++;
                continue;
            }
            List<String> stages = dueStages(due.endsAt(), now);
            if (stages.isEmpty()) continue;
            String urgent = stages.get(0);
            for (String stage : stages) {
                boolean claimed = notices.claim(due.subscriptionId(), stage, now);
                if (claimed && stage.equals(urgent)) {
                    enqueue(due, stage, contact.get(), now);
                    queued++;
                }
            }
        }
        if (withoutContact > 0) {
            log.info("Subscription expiry notices waiting for contact details count={}", withoutContact);
        }
        return queued;
    }

    /** Stages that have fallen due, most urgent first. */
    static List<String> dueStages(Instant endsAt, Instant now) {
        List<String> stages = new ArrayList<>();
        if (!endsAt.isAfter(now)) stages.add(EXPIRED);
        if (!endsAt.isAfter(now.plus(DAY))) stages.add(ONE_DAY);
        if (!endsAt.isAfter(now.plus(WEEK))) stages.add(SEVEN_DAYS);
        return stages;
    }

    private void enqueue(DueSubscription due, String stage, Contact contact, Instant now) {
        UUID eventId = UUID.randomUUID();
        var event = new SubscriptionExpiryNoticeV1(eventId, SubscriptionExpiryNoticeV1.EVENT_TYPE, now, due.userId(),
                due.subscriptionId(), stage, due.endsAt(), contact.email(), contact.displayName());
        try {
            outboxEventRepository.save(OutboxEvent.create(eventId, AGGREGATE_TYPE, due.subscriptionId(),
                    OUTBOX_EVENT_TYPE, objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException serializationFailure) {
            throw new IllegalStateException("Could not serialize subscription expiry notice", serializationFailure);
        }
    }
}
