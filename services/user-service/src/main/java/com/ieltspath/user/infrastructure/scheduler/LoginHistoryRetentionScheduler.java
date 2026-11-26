package com.ieltspath.user.infrastructure.scheduler;

import com.ieltspath.user.application.usecase.LoginHistoryRetentionUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Deletes expired sign-in history in bounded batches; deletion is dry-run by default. */
@Slf4j
@Component
public class LoginHistoryRetentionScheduler {
    private final LoginHistoryRetentionUseCase retentionUseCase;
    private final int retentionDays;
    private final int batchSize;
    private final boolean dryRun;

    public LoginHistoryRetentionScheduler(
            LoginHistoryRetentionUseCase retentionUseCase,
            @Value("${user.login-history.retention-days:180}") int retentionDays,
            @Value("${user.login-history.batch-size:1000}") int batchSize,
            @Value("${user.login-history.dry-run:true}") boolean dryRun
    ) {
        this.retentionUseCase = retentionUseCase;
        this.retentionDays = retentionDays;
        this.batchSize = batchSize;
        this.dryRun = dryRun;
    }

    @Scheduled(fixedDelayString = "${user.login-history.interval:PT24H}")
    public void purge() {
        long count = retentionUseCase.execute(retentionDays, batchSize, dryRun, LocalDateTime.now());
        log.info("Login history retention count={}", count);
    }
}
