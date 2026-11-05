package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Keeps login history (which holds addresses and user agents) only as long as the retention period allows. */
@Service
@RequiredArgsConstructor
public class PurgeLoginHistoryUseCase {
    private final LoginHistoryRepository loginHistoryRepository;

    /** Removes attempts older than {@code retentionDays} before {@code now} and returns how many were removed. */
    @Transactional
    public int execute(int retentionDays, LocalDateTime now) {
        if (retentionDays < 1) {
            throw new IllegalArgumentException("Thời hạn lưu lịch sử đăng nhập phải từ 1 ngày trở lên");
        }
        return loginHistoryRepository.deleteOlderThan(now.minusDays(retentionDays));
    }
}
