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

    /** The subset of {@code codes} that exists and is {@code ACTIVE}. */
    Set<String> findActiveCodes(Collection<String> codes);
}
