package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.GrantSubscriptionCommand;
import com.ieltspath.access.application.result.SubscriptionResult;
import com.ieltspath.access.domain.aggregate.Plan;
import com.ieltspath.access.domain.aggregate.Subscription;
import com.ieltspath.access.domain.entity.OutboxEvent;
import com.ieltspath.access.domain.entity.AdminGrantRequest;
import com.ieltspath.access.domain.exception.PlanNotFoundException;
import com.ieltspath.access.domain.exception.DuplicateIdempotencyException;
import com.ieltspath.access.application.exception.InvalidIdempotencyKeyException;
import com.ieltspath.access.domain.repository.AdminGrantRequestRepository;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
import com.ieltspath.access.domain.repository.PlanRepository;
import com.ieltspath.access.domain.repository.SubscriptionRepository;
import com.ieltspath.access.domain.vo.SubscriptionSourceType;
import com.ieltspath.events.access.SubscriptionGrantedV1;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GrantSubscriptionUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final AdminGrantRequestRepository grantRequests;

    @Autowired
    public GrantSubscriptionUseCase(
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            AdminGrantRequestRepository grantRequests
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.grantRequests = grantRequests;
    }

    /** Compatibility constructor for older isolated use-case tests; Spring uses the persistent repository constructor. */
    public GrantSubscriptionUseCase(SubscriptionRepository subscriptionRepository, PlanRepository planRepository,
                                    OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.grantRequests = null;
    }

    public SubscriptionResult execute(GrantSubscriptionCommand command) {
        String key = command.idempotencyKey();
        String requestHash = null;
        if (grantRequests != null) {
            if (key == null || key.isBlank() || key.length() > 100) throw new InvalidIdempotencyKeyException();
            requestHash = requestHash(command);
            Optional<AdminGrantRequest> existing = grantRequests.find(key);
            if (existing.isPresent()) {
                AdminGrantRequest saved = existing.get();
                if (!saved.requestHash().equals(requestHash)) throw new DuplicateIdempotencyException(key);
                Subscription subscription = subscriptionRepository.findById(saved.subscriptionId())
                        .orElseThrow(() -> new IllegalStateException("Reserved subscription no longer exists"));
                Plan originalPlan = planRepository.findById(subscription.getPlanId())
                        .orElseThrow(() -> new PlanNotFoundException(subscription.getPlanId()));
                return result(subscription, originalPlan);
            }
        }

        Plan plan = planRepository.findById(command.planId())
                .orElseThrow(() -> new PlanNotFoundException(command.planId()));

        Optional<Subscription> activeSub = subscriptionRepository.findActiveByUserId(command.userId());
        Subscription subscription;
        if (activeSub.isPresent()) {
            subscription = activeSub.get();
            subscription.extend(command.durationDays(), command.humanGradingCredits());
        } else {
            subscription = Subscription.create(
                    command.userId(),
                    command.planId(),
                    SubscriptionSourceType.ADMIN,
                    UUID.randomUUID(),
                    command.durationDays(),
                    command.humanGradingCredits()
            );
        }
        Subscription saved = subscriptionRepository.save(subscription);

        if (grantRequests != null && !grantRequests.reserve(key, requestHash, saved.getId())) {
            throw new DuplicateIdempotencyException(key);
        }
        if (grantRequests != null) {
            saved = subscriptionRepository.findById(saved.getId())
                    .orElseThrow(() -> new IllegalStateException("Saved subscription could not be read back"));
        }

        UUID eventId = UUID.randomUUID();
        outboxEventRepository.save(OutboxEvent.create(
                eventId, "Subscription", saved.getId(), "SubscriptionGranted",
                toPayload(new SubscriptionGrantedV1(eventId, SubscriptionGrantedV1.EVENT_TYPE, Instant.now(),
                        command.userId(), saved.getId(), plan.getCode(), databasePrecision(saved.getEndsAt())))
        ));

        return result(saved, plan);
    }

    private static SubscriptionResult result(Subscription saved, Plan plan) {
        return new SubscriptionResult(
                saved.getId(),
                saved.getUserId(),
                saved.getPlanId(),
                plan.getCode(),
                plan.getName(),
                saved.getStatus(),
                databasePrecision(saved.getStartsAt()),
                databasePrecision(saved.getEndsAt()),
                saved.getHumanGradingCreditsTotal(),
                saved.getHumanGradingCreditsUsed(),
                saved.getRemainingCredits(),
                databasePrecision(saved.getCreatedAt()),
                databasePrecision(saved.getUpdatedAt())
        );
    }

    private static Instant databasePrecision(Instant value) {
        if (value == null) return null;
        long micros = (value.getNano() + 500L) / 1_000L;
        return Instant.ofEpochSecond(value.getEpochSecond() + micros / 1_000_000L,
                (micros % 1_000_000L) * 1_000L);
    }

    private static String requestHash(GrantSubscriptionCommand command) {
        String input = command.userId() + "|" + command.planId() + "|" + command.durationDays() + "|"
                + command.humanGradingCredits();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private String toPayload(SubscriptionGrantedV1 event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Subscription granted event could not be serialized", exception);
        }
    }
}
