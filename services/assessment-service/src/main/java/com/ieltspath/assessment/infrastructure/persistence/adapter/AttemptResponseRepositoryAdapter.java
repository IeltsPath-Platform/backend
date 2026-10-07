package com.ieltspath.assessment.infrastructure.persistence.adapter;
import com.ieltspath.assessment.domain.entity.AttemptResponse;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import com.ieltspath.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.ieltspath.assessment.infrastructure.persistence.repository.AttemptResponseJpaRepository;
import org.springframework.stereotype.Component;
import java.util.Collection; import java.util.List; import java.util.Optional; import java.util.UUID;
@Component public class AttemptResponseRepositoryAdapter implements AttemptResponseRepository {
 private final AttemptResponseJpaRepository repository; private final AssessmentPersistenceMapper mapper;
 public AttemptResponseRepositoryAdapter(AttemptResponseJpaRepository repository, AssessmentPersistenceMapper mapper){this.repository=repository;this.mapper=mapper;}
 public AttemptResponse save(AttemptResponse value){return mapper.toDomain(repository.save(mapper.toEntity(value)));}
 public Optional<AttemptResponse> findByAttemptItemId(UUID id){return repository.findByAttemptItemId(id).map(mapper::toDomain);}
 public List<AttemptResponse> findByAttemptItemIds(Collection<UUID> ids){return ids.isEmpty()?List.of():repository.findByAttemptItemIdIn(ids).stream().map(mapper::toDomain).toList();}
}
