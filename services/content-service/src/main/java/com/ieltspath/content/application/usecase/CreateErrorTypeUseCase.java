package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.application.service.TrapKnowledgePointSync;
import com.ieltspath.content.domain.vo.ErrorCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreateErrorTypeUseCase {
    private final ErrorTypeRepository errorTypes;
    private final TrapKnowledgePointSync trapKnowledgePointSync;

    public CreateErrorTypeUseCase(ErrorTypeRepository errorTypes, TrapKnowledgePointSync trapKnowledgePointSync) {
        this.errorTypes = errorTypes;
        this.trapKnowledgePointSync = trapKnowledgePointSync;
    }

    public ErrorType execute(String code, String name, ErrorCategory category, int severity,
                             UUID knowledgePointId, String fixHint) {
        ErrorType candidate = ErrorType.create(code, name, category, severity, knowledgePointId, fixHint);
        if (errorTypes.existsByCode(code)) {
            throw new DuplicateCodeException("ErrorType", code);
        }
        trapKnowledgePointSync.sync(candidate);
        return errorTypes.save(candidate);
    }
}
