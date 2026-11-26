package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Retains only the configured window of sign-in history, deleting in short independent batches. */
@Service
@RequiredArgsConstructor
public class LoginHistoryRetentionUseCase {
    private final LoginHistoryRepository loginHistoryRepository;

    /** Returns deleted rows, or eligible rows when dry-run is enabled. */
    public long execute(int retentionDays, int batchSize, boolean dryRun, LocalDateTime now) {
        if (retentionDays < 1) throw new IllegalArgumentException("Retention days must be at least one");
        if (batchSize < 1) throw new IllegalArgumentException("Batch size must be at least one");

        LocalDateTime cutoff = now.minusDays(retentionDays);
        if (dryRun) return loginHistoryRepository.countOlderThan(cutoff);

        long removed = 0;
        int batch;
        do {
            batch = loginHistoryRepository.deleteOlderThan(cutoff, batchSize);
            removed += batch;
        } while (batch == batchSize);
        return removed;
    }
}
