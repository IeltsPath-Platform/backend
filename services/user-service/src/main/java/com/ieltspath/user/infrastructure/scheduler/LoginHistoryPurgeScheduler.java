package com.ieltspath.user.infrastructure.scheduler;

import com.ieltspath.user.application.usecase.PurgeLoginHistoryUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Deletes login history past its retention period once a day. Both values can be set per environment:
 * {@code app.login-history.retention-days} (default 90) and {@code app.login-history.purge-cron}
 * (default 03:30 every day).
 */
@Slf4j
@Component
public class LoginHistoryPurgeScheduler {
    private final PurgeLoginHistoryUseCase purgeLoginHistoryUseCase;
    private final int retentionDays;

    public LoginHistoryPurgeScheduler(
            PurgeLoginHistoryUseCase purgeLoginHistoryUseCase,
            @Value("${app.login-history.retention-days:90}") int retentionDays
    ) {
        this.purgeLoginHistoryUseCase = purgeLoginHistoryUseCase;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${app.login-history.purge-cron:0 30 3 * * *}")
    public void purge() {
        int removed = purgeLoginHistoryUseCase.execute(retentionDays, LocalDateTime.now());
        log.info("Đã xóa {} lượt lịch sử đăng nhập cũ hơn {} ngày", removed, retentionDays);
    }
}
