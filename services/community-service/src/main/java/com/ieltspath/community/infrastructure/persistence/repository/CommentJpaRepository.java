package com.ieltspath.community.infrastructure.persistence.repository;

import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.infrastructure.persistence.entity.CommentJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, UUID> {
    Page<CommentJpaEntity> findByPostIdAndStatus(UUID postId, ContentStatus status, Pageable pageable);

    @Query(
            value = """
                    SELECT * FROM comments c
                    WHERE c.post_id = :postId
                      AND c.status = :status
                      AND NOT EXISTS (
                          SELECT 1 FROM user_blocks ub
                          WHERE (ub.blocker_id = :viewer AND ub.blocked_id = c.author_id)
                             OR (ub.blocker_id = c.author_id AND ub.blocked_id = :viewer)
                      )
                    """,
            countQuery = """
                    SELECT count(*) FROM comments c
                    WHERE c.post_id = :postId
                      AND c.status = :status
                      AND NOT EXISTS (
                          SELECT 1 FROM user_blocks ub
                          WHERE (ub.blocker_id = :viewer AND ub.blocked_id = c.author_id)
                             OR (ub.blocker_id = c.author_id AND ub.blocked_id = :viewer)
                      )
                    """,
            nativeQuery = true
    )
    Page<CommentJpaEntity> findByPostIdAndStatusVisibleTo(
            @Param("postId") UUID postId,
            @Param("status") String status,
            @Param("viewer") UUID viewer,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from CommentJpaEntity c where c.id=:id")
    Optional<CommentJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
