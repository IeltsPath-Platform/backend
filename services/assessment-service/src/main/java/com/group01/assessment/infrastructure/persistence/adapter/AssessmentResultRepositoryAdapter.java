package com.group01.assessment.infrastructure.persistence.adapter;
import com.group01.assessment.domain.entity.AssessmentResult;
import com.group01.assessment.domain.repository.AssessmentResultRepository;
import com.group01.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.group01.assessment.infrastructure.persistence.repository.AssessmentResultJpaRepository;
import org.springframework.stereotype.Component;
import java.util.Optional; import java.util.UUID;
@Component public class AssessmentResultRepositoryAdapter implements AssessmentResultRepository {
 private final AssessmentResultJpaRepository repository; private final AssessmentPersistenceMapper mapper;
 public AssessmentResultRepositoryAdapter(AssessmentResultJpaRepository repository, AssessmentPersistenceMapper mapper){this.repository=repository;this.mapper=mapper;}
 public AssessmentResult save(AssessmentResult value){return mapper.toDomain(repository.save(mapper.toEntity(value)));}
 public Optional<AssessmentResult> findLatestByAttemptId(UUID id){return repository.findTopByAttemptIdOrderByResultVersionDesc(id).map(mapper::toDomain);}
}
