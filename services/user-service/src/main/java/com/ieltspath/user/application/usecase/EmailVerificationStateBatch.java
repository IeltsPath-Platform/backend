package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.UserEmailVerificationStateV1;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Publishes one batch of verification-state events in its own transaction. It is a separate bean so the backfill runner
 * reaches it through the transaction proxy.
 */
@Component
@RequiredArgsConstructor
class EmailVerificationStateBatch {
    private final UserRepository userRepository;
    private final UserNoticeOutbox notices;

    /** Queues the state for up to {@code limit} accounts after {@code after}; returns the last id, or null when none remain. */
    @Transactional
    UUID publishAfter(UUID after, int limit) {
        List<User> users = userRepository.findAfterId(after == null ? new UUID(0L, 0L) : after, limit);
        UUID last = null;
        for (User user : users) {
            UUID stateId = UUID.randomUUID();
            notices.enqueue(stateId, user.getId(), UserEmailVerificationStateV1.EVENT_TYPE,
                    new UserEmailVerificationStateV1(stateId, UserEmailVerificationStateV1.EVENT_TYPE, Instant.now(),
                            user.getId(), user.isEmailVerified(), user.getEmail().value(), user.getFullName()));
            last = user.getId();
        }
        return last;
    }
}
