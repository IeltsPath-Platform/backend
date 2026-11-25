package com.ieltspath.user.infrastructure.scheduler;

import com.ieltspath.user.application.usecase.PurgeAuthAuditUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthAuditPurgeScheduler {
    private final PurgeAuthAuditUseCase purge;
    @Value("${app.auth.audit.retention-days:365}") private int retentionDays;
    @Value("${app.auth.audit.batch-size:500}") private int batchSize;

    @Scheduled(cron = "${app.auth.audit.purge-cron:0 40 3 * * *}")
    public void purge() {
        int removed = purge.execute(retentionDays, batchSize, LocalDateTime.now());
        if (removed > 0) log.info("Đã xóa {} sự kiện xác thực quá hạn lưu trữ", removed);
    }
}
