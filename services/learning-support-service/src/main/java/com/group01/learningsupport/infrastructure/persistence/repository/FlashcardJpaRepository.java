package com.group01.learningsupport.infrastructure.persistence.repository;

import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FlashcardJpaRepository extends JpaRepository<FlashcardJpaEntity, UUID> {
    Optional<FlashcardJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Page<FlashcardJpaEntity> findByUserIdAndStatus(UUID userId, LibraryStatus status, Pageable pageable);
}
