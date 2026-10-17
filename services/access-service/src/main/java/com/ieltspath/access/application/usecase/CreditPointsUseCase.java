package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.CreditPointsCommand;
import com.ieltspath.access.application.result.PointCreditResult;
import com.ieltspath.access.domain.exception.PointsActorMismatchException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CreditPointsUseCase {
    private final CreditPointsWriter writer;

    public CreditPointsUseCase(CreditPointsWriter writer) {
        this.writer = writer;
    }

    public PointCreditResult execute(CreditPointsCommand command) {
        if (!Objects.equals(command.actorUserId(), command.userId())) throw new PointsActorMismatchException();
        if (command.amount() < 1 || command.amount() > 100 || command.reason() == null
                || command.referenceId() == null || command.referenceId().isBlank()
                || command.referenceId().length() > 100 || command.idempotencyKey() == null
                || command.idempotencyKey().isBlank() || command.idempotencyKey().length() > 120) {
            throw new IllegalArgumentException("Invalid point credit request");
        }
        return writer.credit(command);
    }
}
