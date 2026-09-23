package com.group01.learningsupport.infrastructure.persistence.repository;

import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardDeckJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FlashcardDeckJpaRepository extends JpaRepository<FlashcardDeckJpaEntity, UUID> {
    Optional<FlashcardDeckJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Page<FlashcardDeckJpaEntity> findByUserIdAndStatus(UUID userId, LibraryStatus status, Pageable pageable);
}
