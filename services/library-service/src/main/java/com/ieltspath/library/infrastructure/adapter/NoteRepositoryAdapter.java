package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.domain.aggregate.Note;
import com.ieltspath.library.domain.repository.NoteRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.OwnedPage;
import com.ieltspath.library.domain.vo.NoteSourceType;
import com.ieltspath.library.infrastructure.persistence.JpaSupport;
import com.ieltspath.library.infrastructure.persistence.entity.NoteJpaEntity;
import com.ieltspath.library.infrastructure.persistence.mapper.NoteMapper;
import com.ieltspath.library.infrastructure.persistence.repository.NoteJpaRepository;
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

    @Override
    public OwnedPage<Note> findByUserIdAndStatusAndSourceType(
            UUID userId, LibraryStatus status, NoteSourceType sourceType, int page, int size) {
        return JpaSupport.page(
                repository.findByUserIdAndStatusAndSourceType(
                        userId, status, sourceType, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))),
                mapper::toDomain
        );
    }

    @Override
    public OwnedPage<Note> findByUserIdAndStatusAndSourceTypeAndSourceReferenceId(
            UUID userId, LibraryStatus status, NoteSourceType sourceType,
            UUID sourceReferenceId, int page, int size) {
        return JpaSupport.page(
                repository.findByUserIdAndStatusAndSourceTypeAndSourceReferenceId(
                        userId, status, sourceType, sourceReferenceId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))),
                mapper::toDomain
        );
    }
}
