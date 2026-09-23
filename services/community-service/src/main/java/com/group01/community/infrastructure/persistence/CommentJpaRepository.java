package com.group01.community.infrastructure.persistence;

import com.group01.community.domain.vo.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, UUID> {
    Page<CommentJpaEntity> findByPostIdAndStatus(UUID postId, ContentStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from CommentJpaEntity c where c.id=:id")
    Optional<CommentJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
