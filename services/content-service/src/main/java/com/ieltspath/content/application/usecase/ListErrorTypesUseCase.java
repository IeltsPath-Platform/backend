package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListErrorTypesUseCase {
    private final ErrorTypeRepository errorTypes;

    public ListErrorTypesUseCase(ErrorTypeRepository errorTypes) {
        this.errorTypes = errorTypes;
    }

    /** Every error type by code, or only the {@code ACTIVE} ones when {@code activeOnly} is set. */
    public List<ErrorType> execute(boolean activeOnly) {
        return activeOnly ? errorTypes.findActive() : errorTypes.findAll();
    }

    /** The {@code ACTIVE} codes whatever their review status: the set a grader may record. */
    public List<ErrorType> executeActive() {
        return errorTypes.findActive();
    }

    /** Returns only reviewed codes that may be included in learner-facing grading output. */
    public List<ErrorType> executeApprovedActive() {
        return errorTypes.findApprovedActive();
    }
}
