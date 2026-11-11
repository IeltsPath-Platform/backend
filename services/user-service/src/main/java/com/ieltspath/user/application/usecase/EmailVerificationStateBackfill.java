package com.ieltspath.user.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * One-time catch-up for access-service: publishes the verification state and contact details of every existing account,
 * so expiry notices can be addressed to accounts created before the event existed. Runs only when
 * {@code user.backfill.verification-state.enabled=true}; the consumer keeps the latest state, so running it twice is safe.
 * Accounts are walked in id order in batches; each batch commits on its own so a long run never holds one transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user.backfill.verification-state.enabled", havingValue = "true")
public class EmailVerificationStateBackfill implements ApplicationRunner {
    private static final int BATCH_SIZE = 500;

    private final EmailVerificationStateBatch batch;

    @Override
    public void run(ApplicationArguments args) {
        UUID after = null;
        int batches = 0;
        while (true) {
            UUID last = batch.publishAfter(after, BATCH_SIZE);
            if (last == null) break;
            after = last;
            batches++;
            log.info("Email verification state backfill batch={} lastAccountId={}", batches, last);
        }
        log.info("Email verification state backfill finished batches={}", batches);
    }
}
