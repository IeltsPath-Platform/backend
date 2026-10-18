package com.ieltspath.assessment.infrastructure.persistence.adapter;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.ieltspath.assessment.infrastructure.persistence.repository.AttemptItemJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
@Component public class AttemptItemRepositoryAdapter implements AttemptItemRepository {
 private final AttemptItemJpaRepository repository; private final AssessmentPersistenceMapper mapper;
 public AttemptItemRepositoryAdapter(AttemptItemJpaRepository repository, AssessmentPersistenceMapper mapper){this.repository=repository;this.mapper=mapper;}
 public List<AttemptItem> saveAll(List<AttemptItem> values){return repository.saveAll(values.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain).toList();}
 public Optional<AttemptItem> findById(UUID id){return repository.findById(id).map(mapper::toDomain);}
 public Optional<AttemptItem> findByIdAndAttemptId(UUID id, UUID attemptId){return repository.findByIdAndAttemptId(id,attemptId).map(mapper::toDomain);}
 public List<AttemptItem> findByAttemptId(UUID id){return repository.findByAttemptId(id).stream().map(mapper::toDomain).toList();}

    public Set<UUID> findExistingIdsByAttemptId(Set<UUID> itemIds, UUID attemptId) {
        return itemIds.isEmpty() ? Set.of() : repository.findExistingIdsByAttemptId(itemIds, attemptId);
    }

    @Override
    public void recordElapsed(UUID attemptItemId, long elapsedMs) {
        repository.findById(attemptItemId).ifPresent(entity -> {
            long stored = entity.getElapsedMs() == null ? 0L : entity.getElapsedMs();
            entity.setElapsedMs(Math.max(stored, elapsedMs));
            repository.save(entity);
        });
    }

    @Override
    public Map<UUID, Long> elapsedMsByItemId(Collection<UUID> attemptItemIds) {
        if (attemptItemIds.isEmpty()) return Map.of();
        Map<UUID, Long> elapsed = new HashMap<>();
        repository.findAllById(attemptItemIds).forEach(entity -> {
            if (entity.getElapsedMs() != null) elapsed.put(entity.getId(), entity.getElapsedMs());
        });
        return elapsed;
    }
}
