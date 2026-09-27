package com.group01.learningsupport.infrastructure.persistence.repository;

import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.domain.vo.NoteSourceType;
import com.group01.learningsupport.infrastructure.persistence.entity.NoteJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NoteJpaRepository extends JpaRepository<NoteJpaEntity, UUID> {
    Optional<NoteJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Page<NoteJpaEntity> findByUserIdAndStatus(UUID userId, LibraryStatus status, Pageable pageable);

    Page<NoteJpaEntity> findByUserIdAndStatusAndSourceType(
            UUID userId, LibraryStatus status, NoteSourceType sourceType, Pageable pageable);

    Page<NoteJpaEntity> findByUserIdAndStatusAndSourceTypeAndSourceReferenceId(
            UUID userId, LibraryStatus status, NoteSourceType sourceType,
            UUID sourceReferenceId, Pageable pageable);
}
