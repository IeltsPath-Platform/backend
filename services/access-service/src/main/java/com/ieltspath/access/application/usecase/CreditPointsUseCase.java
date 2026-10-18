package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.CreditPointsCommand;
import com.ieltspath.access.application.result.PointCreditResult;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CreditPointsUseCase {
    private final CreditPointsWriter writer;

    public CreditPointsUseCase(CreditPointsWriter writer) {
        this.writer = writer;
    }

    public PointCreditResult executeLearningReward(UUID grantId, UUID userId, int amount,
                                                   com.ieltspath.access.domain.vo.LearningRewardReason reason,
                                                   String referenceId) {
        if (grantId == null || userId == null) throw new IllegalArgumentException("Invalid point grant request");
        return creditValidated(new CreditPointsCommand(userId, userId, amount, reason, referenceId,
                grantId.toString()));
    }

    private PointCreditResult creditValidated(CreditPointsCommand command) {
        if (command.amount() < 1 || command.amount() > 100 || command.reason() == null
                || command.referenceId() == null || command.referenceId().isBlank()
                || command.referenceId().length() > 100 || command.idempotencyKey() == null
                || command.idempotencyKey().isBlank() || command.idempotencyKey().length() > 120) {
            throw new IllegalArgumentException("Invalid point credit request");
        }
        return writer.credit(command);
    }
}
