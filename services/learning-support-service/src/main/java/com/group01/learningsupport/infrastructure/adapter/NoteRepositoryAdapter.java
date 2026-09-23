package com.group01.learningsupport.infrastructure.adapter;

import com.group01.learningsupport.domain.aggregate.Note;
import com.group01.learningsupport.domain.repository.NoteRepository;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.domain.vo.OwnedPage;
import com.group01.learningsupport.infrastructure.persistence.JpaSupport;
import com.group01.learningsupport.infrastructure.persistence.entity.NoteJpaEntity;
import com.group01.learningsupport.infrastructure.persistence.mapper.NoteMapper;
import com.group01.learningsupport.infrastructure.persistence.repository.NoteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NoteRepositoryAdapter implements NoteRepository {
    private final NoteJpaRepository repository;
    private final NoteMapper mapper;

    @Override
    public Note save(Note note) {
        NoteJpaEntity entity = repository.findById(note.getId()).orElseGet(NoteJpaEntity::new);
        mapper.copy(note, entity);
        return mapper.toDomain(JpaSupport.flush(repository, entity));
    }

    @Override
    public Optional<Note> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public OwnedPage<Note> findByUserIdAndStatus(UUID userId, LibraryStatus status, int page, int size) {
        return JpaSupport.page(
                repository.findByUserIdAndStatus(userId, status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))),
                mapper::toDomain
        );
    }
}
