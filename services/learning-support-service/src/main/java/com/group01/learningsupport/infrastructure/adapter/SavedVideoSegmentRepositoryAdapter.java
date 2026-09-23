package com.group01.learningsupport.infrastructure.adapter;

import com.group01.learningsupport.domain.aggregate.SavedVideoSegment;
import com.group01.learningsupport.domain.repository.SavedVideoSegmentRepository;
import com.group01.learningsupport.domain.vo.OwnedPage;
import com.group01.learningsupport.infrastructure.persistence.JpaSupport;
import com.group01.learningsupport.infrastructure.persistence.entity.SavedVideoSegmentJpaEntity;
import com.group01.learningsupport.infrastructure.persistence.mapper.SavedVideoSegmentMapper;
import com.group01.learningsupport.infrastructure.persistence.repository.SavedVideoSegmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SavedVideoSegmentRepositoryAdapter implements SavedVideoSegmentRepository {
    private final SavedVideoSegmentJpaRepository repository;
    private final SavedVideoSegmentMapper mapper;

    @Override
    public SavedVideoSegment save(SavedVideoSegment segment) {
        SavedVideoSegmentJpaEntity entity = repository.findById(segment.getId()).orElseGet(SavedVideoSegmentJpaEntity::new);
        mapper.copy(segment, entity);
        return mapper.toDomain(JpaSupport.flush(repository, entity));
    }

    @Override
    public OwnedPage<SavedVideoSegment> findByUserId(UUID userId, int page, int size) {
        return JpaSupport.page(
                repository.findByUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
                mapper::toDomain
        );
    }

    @Override
    public Optional<SavedVideoSegment> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public void delete(SavedVideoSegment segment) {
        repository.deleteById(segment.getId());
    }
}
