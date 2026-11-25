package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.AuthAuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PurgeAuthAuditUseCase {
    private final AuthAuditEventRepository repository;

    @Transactional
    public int execute(int retentionDays, int batchSize, LocalDateTime now) {
        if (retentionDays < 1 || batchSize < 1 || batchSize > 5000) {
            throw new IllegalArgumentException("Thời hạn hoặc số lượng xóa sự kiện bảo mật không hợp lệ.");
        }
        return repository.deleteExpiredBatch(now.minusDays(retentionDays), batchSize);
    }
}
