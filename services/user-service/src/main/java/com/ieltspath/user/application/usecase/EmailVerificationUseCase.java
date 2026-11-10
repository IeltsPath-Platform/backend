package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.EmailVerificationRequestedV1;
import com.ieltspath.events.user.UserEmailVerificationStateV1;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.AccountActionToken;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.InvalidActionTokenException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.AccountActionTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Email ownership proof for accounts. A verification link is single-use and expires; a resend is rate limited per
 * account and answers the same way whether or not the address exists.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationUseCase {
    private static final int TOKEN_LIFETIME_MINUTES = 60;
    private static final int COOLDOWN_SECONDS = 60;

    private final UserRepository userRepository;
    private final AccountActionTokenRepository accountActionTokenRepository;
    private final TokenHashService tokenHashService;
    private final UserLock userLock;
    private final UserNoticeOutbox notices;

    /** Issues a fresh verification link for the account and queues the email. Earlier unused links stop working. */
    @Transactional
    public void issue(User user) {
        LocalDateTime now = LocalDateTime.now();
        accountActionTokenRepository.invalidateUnusedForUser(user.getId(), ActionTokenPurpose.EMAIL_VERIFICATION, now);
        String rawToken = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(TOKEN_LIFETIME_MINUTES * 60L);
        accountActionTokenRepository.save(AccountActionToken.builder()
                .userId(user.getId())
                .purpose(ActionTokenPurpose.EMAIL_VERIFICATION)
                .tokenHash(tokenHashService.hash(rawToken))
                .expiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
                .createdAt(now)
                .build());
        UUID eventId = UUID.randomUUID();
        notices.enqueue(eventId, user.getId(), EmailVerificationRequestedV1.EVENT_TYPE, new EmailVerificationRequestedV1(
                eventId, EmailVerificationRequestedV1.EVENT_TYPE, Instant.now(), user.getId(),
                user.getEmail().value(), user.getFullName(), rawToken, expiresAt));
        publishState(user, false);
    }

    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token không được để trống");
        }
        String tokenHash = tokenHashService.hash(rawToken.trim());
        AccountActionToken token = accountActionTokenRepository
                .findByTokenHashAndPurpose(tokenHash, ActionTokenPurpose.EMAIL_VERIFICATION)
                .orElseThrow(() -> new InvalidActionTokenException("Liên kết xác minh không hợp lệ"));
        if (!token.isValid()) {
            throw new InvalidActionTokenException("Liên kết xác minh đã hết hạn hoặc đã được sử dụng");
        }
        userLock.lock(token.getUserId());
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng sở hữu liên kết này"));
        LocalDateTime now = LocalDateTime.now();
        if (!accountActionTokenRepository.consumeToken(tokenHash, ActionTokenPurpose.EMAIL_VERIFICATION, now)) {
            throw new InvalidActionTokenException("Liên kết xác minh đã hết hạn hoặc đã được sử dụng");
        }
        user.markEmailVerified(now);
        userRepository.save(user);
        publishState(user, true);
    }

    /** Public resend: silently does nothing for unknown, active-verified, inactive or recently-sent cases. */
    @Transactional
    public void resend(String email) {
        Email parsed;
        try {
            parsed = new Email(email);
        } catch (IllegalArgumentException | NullPointerException invalidEmail) {
            return;
        }
        User found = userRepository.findByEmail(parsed.value()).orElse(null);
        if (found == null) return;
        userLock.lock(found.getId());
        User user = userRepository.findById(found.getId()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE || user.isEmailVerified()) return;
        LocalDateTime now = LocalDateTime.now();
        if (accountActionTokenRepository.findLatestCreatedAt(user.getId(), ActionTokenPurpose.EMAIL_VERIFICATION)
                .filter(createdAt -> createdAt.isAfter(now.minusSeconds(COOLDOWN_SECONDS))).isPresent()) {
            return;
        }
        issue(user);
    }

    /** Tells access-service the account's verification state and contact details; the consumer keeps the latest by time. */
    private void publishState(User user, boolean verified) {
        UUID stateId = UUID.randomUUID();
        notices.enqueue(stateId, user.getId(), UserEmailVerificationStateV1.EVENT_TYPE, new UserEmailVerificationStateV1(
                stateId, UserEmailVerificationStateV1.EVENT_TYPE, Instant.now(), user.getId(), verified,
                user.getEmail().value(), user.getFullName()));
    }
}
