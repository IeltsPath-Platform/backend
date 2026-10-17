package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.ErrorType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ErrorTypeRepository {
    ErrorType save(ErrorType errorType);

    Optional<ErrorType> findById(UUID id);

    boolean existsByCode(String code);

    List<ErrorType> findAll();

    List<ErrorType> findActive();

    /** Error types safe for grading output: both active and reviewed by a person. */
    List<ErrorType> findApprovedActive();

    /** The subset of {@code codes} that exists, is {@code ACTIVE}, and has been approved. */
    Set<String> findActiveCodes(Collection<String> codes);
}
