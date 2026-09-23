package com.group01.assessment.infrastructure.persistence.adapter;
import com.group01.assessment.domain.entity.GradingJob;
import com.group01.assessment.domain.repository.GradingJobRepository;
import com.group01.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.group01.assessment.infrastructure.persistence.repository.GradingJobJpaRepository;
import org.springframework.stereotype.Component;
import java.util.Optional; import java.util.UUID;
@Component public class GradingJobRepositoryAdapter implements GradingJobRepository {
 private final GradingJobJpaRepository repository; private final AssessmentPersistenceMapper mapper;
 public GradingJobRepositoryAdapter(GradingJobJpaRepository repository, AssessmentPersistenceMapper mapper){this.repository=repository;this.mapper=mapper;}
 public GradingJob save(GradingJob value){return mapper.toDomain(repository.save(mapper.toEntity(value)));}
 public Optional<GradingJob> findById(UUID id){return repository.findById(id).map(mapper::toDomain);}
}
