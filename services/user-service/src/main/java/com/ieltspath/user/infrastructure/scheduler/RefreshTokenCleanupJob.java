package com.ieltspath.user.infrastructure.scheduler;

import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {
    private final RefreshTokenRepository refreshTokens;

    @Scheduled(cron = "${app.auth.refresh-token-cleanup-cron:0 15 3 * * *}")
    @Transactional
    public void clean() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokens.deleteExpiredOrOldRevoked(now, now.minusDays(30), 1000);
    }
}
