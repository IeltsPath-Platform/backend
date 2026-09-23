package com.group01.assessment.infrastructure.persistence.adapter;
import com.group01.assessment.domain.entity.AttemptSection;
import com.group01.assessment.domain.repository.AttemptSectionRepository;
import com.group01.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.group01.assessment.infrastructure.persistence.repository.AttemptSectionJpaRepository;
import org.springframework.stereotype.Component;
import java.util.List; import java.util.Optional; import java.util.UUID;
@Component public class AttemptSectionRepositoryAdapter implements AttemptSectionRepository {
 private final AttemptSectionJpaRepository repository; private final AssessmentPersistenceMapper mapper;
 public AttemptSectionRepositoryAdapter(AttemptSectionJpaRepository repository, AssessmentPersistenceMapper mapper){this.repository=repository;this.mapper=mapper;}
 public List<AttemptSection> saveAll(List<AttemptSection> values){return repository.saveAll(values.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain).toList();}
 public Optional<AttemptSection> findById(UUID id){return repository.findById(id).map(mapper::toDomain);}
 public List<AttemptSection> findByAttemptId(UUID id){return repository.findByAttemptIdOrderBySortOrderAsc(id).stream().map(mapper::toDomain).toList();}
}
