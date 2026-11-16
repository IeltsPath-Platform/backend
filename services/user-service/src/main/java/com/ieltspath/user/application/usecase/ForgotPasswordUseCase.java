package com.ieltspath.user.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.user.PasswordResetGoogleOnlyV1;
import com.ieltspath.events.user.PasswordResetRequestedV1;
import com.ieltspath.user.application.command.ForgotPasswordCommand;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.AccountActionToken;
import com.ieltspath.user.domain.aggregate.OutboxEvent;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.AccountActionTokenRepository;
import com.ieltspath.user.domain.repository.OAuthIdentityRepository;
import com.ieltspath.user.domain.repository.OutboxEventRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.OAuthProvider;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordUseCase {
    private static final int TOKEN_LIFETIME_MINUTES = 15;
    private static final int COOLDOWN_SECONDS = 60;
    /** One "use Google" notice per account in this window: the id repeats inside it and Notification mails an id once. */
    private static final long GOOGLE_ONLY_NOTICE_WINDOW_SECONDS = 15 * 60L;

    private final UserRepository userRepository;
    private final AccountActionTokenRepository accountActionTokenRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TokenHashService tokenHashService;
    private final UserLock userLock;
    private final ObjectMapper objectMapper;
    private final OAuthIdentityRepository oauthIdentityRepository;
    private final UserNoticeOutbox notices;

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

        // An account created by Google sign-in has no password to reset. Say so by mail instead of issuing a link that
        // would quietly add a second login method. An account whose password was removed when Google was linked still
        // gets a link, so its owner can choose a password again.
        if (user.hasNeverHadPassword()
                && oauthIdentityRepository.findByUserIdAndProvider(user.getId(), OAuthProvider.GOOGLE).isPresent()) {
            queueGoogleOnlyNotice(user, email);
            return;
        }

        // Token timestamps are all UTC: expiresAt is stored in UTC and compared against this clock on consume.
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (accountActionTokenRepository.findLatestCreatedAt(user.getId(), ActionTokenPurpose.PASSWORD_RESET)
                .filter(createdAt -> createdAt.isAfter(now.minusSeconds(COOLDOWN_SECONDS))).isPresent()) {
            return;
        }

        accountActionTokenRepository.invalidateUnusedForUser(user.getId(), ActionTokenPurpose.PASSWORD_RESET, now);
        String rawToken = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(TOKEN_LIFETIME_MINUTES * 60L);
        accountActionTokenRepository.save(AccountActionToken.builder()
                // No id here: the entity generates it, and a preset id makes JPA merge instead of insert.
                .userId(user.getId()).purpose(ActionTokenPurpose.PASSWORD_RESET)
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

    private void queueGoogleOnlyNotice(User user, Email email) {
        long window = Instant.now().getEpochSecond() / GOOGLE_ONLY_NOTICE_WINDOW_SECONDS;
        UUID eventId = UUID.nameUUIDFromBytes(("password-reset-google-only:" + user.getId() + ":" + window)
                .getBytes(StandardCharsets.UTF_8));
        notices.enqueue(eventId, user.getId(), PasswordResetGoogleOnlyV1.EVENT_TYPE, new PasswordResetGoogleOnlyV1(
                eventId, PasswordResetGoogleOnlyV1.EVENT_TYPE, Instant.now(), user.getId(), email.value(), user.getFullName()));
        log.info("Password reset skipped for a Google-only account userId={}", user.getId());
    }
}
