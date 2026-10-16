package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.exception.ErrorTypeNotFoundException;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.ErrorCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Revises an error type. The code is not part of the update: stored answer specs refer to it. */
@Service
@Transactional
public class UpdateErrorTypeUseCase {
    private final ErrorTypeRepository errorTypes;

    public UpdateErrorTypeUseCase(ErrorTypeRepository errorTypes) {
        this.errorTypes = errorTypes;
    }

    public ErrorType execute(UUID id, String name, ErrorCategory category, int severity, UUID knowledgePointId,
                             String fixHint, ContentStatus status) {
        ErrorType errorType = errorTypes.findById(id).orElseThrow(() -> new ErrorTypeNotFoundException(id));
        errorType.update(name, category, severity, knowledgePointId, fixHint, status);
        return errorTypes.save(errorType);
    }
}
