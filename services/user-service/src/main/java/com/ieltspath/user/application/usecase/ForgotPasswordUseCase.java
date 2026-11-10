package com.ieltspath.user.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.user.PasswordResetRequestedV1;
import com.ieltspath.user.application.command.ForgotPasswordCommand;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.AccountActionToken;
import com.ieltspath.user.domain.aggregate.OutboxEvent;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.AccountActionTokenRepository;
import com.ieltspath.user.domain.repository.OutboxEventRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordUseCase {
    private static final int TOKEN_LIFETIME_MINUTES = 15;
    private static final int COOLDOWN_SECONDS = 60;

    private final UserRepository userRepository;
    private final AccountActionTokenRepository accountActionTokenRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TokenHashService tokenHashService;
    private final UserLock userLock;
    private final ObjectMapper objectMapper;

    @Transactional
    public void execute(ForgotPasswordCommand command) {
        Email email;
        try {
            email = new Email(command.email());
        } catch (IllegalArgumentException | NullPointerException invalidEmail) {
            return;
        }

        User found = userRepository.findByEmail(email.value()).orElse(null);
        if (found == null) return;
        userLock.lock(found.getId());
        User user = userRepository.findById(found.getId()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) return;

        LocalDateTime now = LocalDateTime.now();
        if (accountActionTokenRepository.findLatestCreatedAt(user.getId(), ActionTokenPurpose.PASSWORD_RESET)
                .filter(createdAt -> createdAt.isAfter(now.minusSeconds(COOLDOWN_SECONDS))).isPresent()) {
            return;
        }

        accountActionTokenRepository.invalidateUnusedForUser(user.getId(), ActionTokenPurpose.PASSWORD_RESET, now);
        String rawToken = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(TOKEN_LIFETIME_MINUTES * 60L);
        accountActionTokenRepository.save(AccountActionToken.builder()
                .id(UUID.randomUUID()).userId(user.getId()).purpose(ActionTokenPurpose.PASSWORD_RESET)
                .tokenHash(tokenHashService.hash(rawToken))
                .expiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)).createdAt(now).build());

        UUID eventId = UUID.randomUUID();
        PasswordResetRequestedV1 event = new PasswordResetRequestedV1(eventId,
                PasswordResetRequestedV1.EVENT_TYPE, Instant.now(), user.getId(), email.value(),
                user.getFullName(), rawToken, expiresAt);
        try {
            outboxEventRepository.save(OutboxEvent.create(eventId, user.getId(), event.eventType(),
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException serializationFailure) {
            throw new IllegalStateException("Could not serialize password reset event", serializationFailure);
        }
        log.info("Password reset requested userId={} eventId={}", user.getId(), eventId);
    }
}
