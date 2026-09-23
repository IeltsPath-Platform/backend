package com.group01.assessment.infrastructure.persistence.adapter;

import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.repository.AssessmentAttemptRepository;
import com.group01.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.group01.assessment.infrastructure.persistence.repository.AssessmentAttemptJpaRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AssessmentAttemptRepositoryAdapter implements AssessmentAttemptRepository {
    private final AssessmentAttemptJpaRepository repository; private final AssessmentPersistenceMapper mapper;
    public AssessmentAttemptRepositoryAdapter(AssessmentAttemptJpaRepository repository, AssessmentPersistenceMapper mapper) { this.repository = repository; this.mapper = mapper; }
    public AssessmentAttempt save(AssessmentAttempt value) { return mapper.toDomain(repository.save(mapper.toEntity(value))); }
    public Optional<AssessmentAttempt> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    public Optional<AssessmentAttempt> findByIdAndUserId(UUID id, UUID userId) { return repository.findByIdAndUserId(id, userId).map(mapper::toDomain); }
    public List<AssessmentAttempt> findByUserId(UUID userId) { return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::toDomain).toList(); }
}
