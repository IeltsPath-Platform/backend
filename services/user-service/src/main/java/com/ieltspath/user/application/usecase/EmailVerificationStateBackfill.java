package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.UserEmailVerificationStateV1;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * One-time catch-up for access-service: publishes the verification state and contact details of every existing account,
 * so expiry notices can be addressed to accounts created before the event existed. Runs only when
 * {@code user.backfill.verification-state.enabled=true}; the consumer keeps the latest state, so running it twice is safe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user.backfill.verification-state.enabled", havingValue = "true")
public class EmailVerificationStateBackfill implements ApplicationRunner {
    private final UserRepository userRepository;
    private final UserNoticeOutbox notices;

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        int published = 0;
        for (User user : userRepository.findAll()) {
            UUID stateId = UUID.randomUUID();
            notices.enqueue(stateId, user.getId(), UserEmailVerificationStateV1.EVENT_TYPE,
                    new UserEmailVerificationStateV1(stateId, UserEmailVerificationStateV1.EVENT_TYPE, Instant.now(),
                            user.getId(), user.isEmailVerified(), user.getEmail().value(), user.getFullName()));
            published++;
        }
        log.info("Email verification state backfill queued accounts={}", published);
    }
}
