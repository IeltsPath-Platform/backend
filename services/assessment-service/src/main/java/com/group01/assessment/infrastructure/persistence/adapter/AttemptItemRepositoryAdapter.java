package com.group01.assessment.infrastructure.persistence.adapter;
import com.group01.assessment.domain.entity.AttemptItem;
import com.group01.assessment.domain.repository.AttemptItemRepository;
import com.group01.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.group01.assessment.infrastructure.persistence.repository.AttemptItemJpaRepository;
import org.springframework.stereotype.Component;
import java.util.List; import java.util.Optional; import java.util.UUID;
@Component public class AttemptItemRepositoryAdapter implements AttemptItemRepository {
 private final AttemptItemJpaRepository repository; private final AssessmentPersistenceMapper mapper;
 public AttemptItemRepositoryAdapter(AttemptItemJpaRepository repository, AssessmentPersistenceMapper mapper){this.repository=repository;this.mapper=mapper;}
 public List<AttemptItem> saveAll(List<AttemptItem> values){return repository.saveAll(values.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain).toList();}
 public Optional<AttemptItem> findById(UUID id){return repository.findById(id).map(mapper::toDomain);}
 public Optional<AttemptItem> findByIdAndAttemptId(UUID id, UUID attemptId){return repository.findByIdAndAttemptId(id,attemptId).map(mapper::toDomain);}
 public List<AttemptItem> findByAttemptId(UUID id){return repository.findByAttemptId(id).stream().map(mapper::toDomain).toList();}
}
