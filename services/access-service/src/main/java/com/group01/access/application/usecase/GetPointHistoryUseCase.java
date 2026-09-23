package com.group01.access.application.usecase;

import com.group01.access.application.result.PointLedgerResult;
import com.group01.access.domain.repository.PointLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetPointHistoryUseCase {

    private final PointLedgerRepository pointLedgerRepository;

    public GetPointHistoryUseCase(PointLedgerRepository pointLedgerRepository) {
        this.pointLedgerRepository = pointLedgerRepository;
    }

    public List<PointLedgerResult> execute(UUID userId, int offset, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safeOffset = Math.max(offset, 0);
        return pointLedgerRepository.findByUserId(userId, safeOffset, safeLimit).stream()
                .map(e -> new PointLedgerResult(
                e.getId(), e.getUserId(), e.getDelta(), e.getBalanceAfter(),
                e.getTransactionType(), e.getReferenceType(), e.getReferenceId(),
                e.getDescription(), e.getCreatedAt()
        ))
                .toList();
    }
}
