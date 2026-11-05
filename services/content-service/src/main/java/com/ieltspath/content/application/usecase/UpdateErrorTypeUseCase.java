package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.exception.ErrorTypeNotFoundException;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.application.service.TrapKnowledgePointSync;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.ErrorCategory;
import com.ieltspath.content.domain.vo.ErrorReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Revises an error type. The code is not part of the update: stored answer specs refer to it. */
@Service
@Transactional
public class UpdateErrorTypeUseCase {
    private final ErrorTypeRepository errorTypes;
    private final TrapKnowledgePointSync trapKnowledgePointSync;

    public UpdateErrorTypeUseCase(ErrorTypeRepository errorTypes, TrapKnowledgePointSync trapKnowledgePointSync) {
        this.errorTypes = errorTypes;
        this.trapKnowledgePointSync = trapKnowledgePointSync;
    }

    public ErrorType execute(UUID id, String name, ErrorCategory category, int severity, UUID knowledgePointId,
                             String fixHint, ContentStatus status, ErrorReviewStatus reviewStatus) {
        ErrorType errorType = errorTypes.findById(id).orElseThrow(() -> new ErrorTypeNotFoundException(id));
        errorType.update(name, category, severity, knowledgePointId, fixHint, status, reviewStatus);
        trapKnowledgePointSync.sync(errorType);
        return errorTypes.save(errorType);
    }
}
