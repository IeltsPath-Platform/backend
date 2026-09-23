package com.group01.assessment.infrastructure.persistence.adapter;
import com.group01.assessment.domain.entity.LearnerSubmission;
import com.group01.assessment.domain.repository.LearnerSubmissionRepository;
import com.group01.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.group01.assessment.infrastructure.persistence.repository.LearnerSubmissionJpaRepository;
import org.springframework.stereotype.Component;
import java.util.Optional; import java.util.UUID;
@Component public class LearnerSubmissionRepositoryAdapter implements LearnerSubmissionRepository {
 private final LearnerSubmissionJpaRepository repository; private final AssessmentPersistenceMapper mapper;
 public LearnerSubmissionRepositoryAdapter(LearnerSubmissionJpaRepository repository, AssessmentPersistenceMapper mapper){this.repository=repository;this.mapper=mapper;}
 public LearnerSubmission save(LearnerSubmission value){return mapper.toDomain(repository.save(mapper.toEntity(value)));}
 public Optional<LearnerSubmission> findById(UUID id){return repository.findById(id).map(mapper::toDomain);}
 public Optional<LearnerSubmission> findByUserIdAndSubmissionKey(UUID userId,String key){return repository.findByUserIdAndSubmissionKey(userId,key).map(mapper::toDomain);}
}
