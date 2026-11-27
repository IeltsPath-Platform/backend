package com.ieltspath.community.infrastructure.persistence.repository;

import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.infrastructure.persistence.entity.PostJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PostJpaRepository extends JpaRepository<PostJpaEntity, UUID> {
    Page<PostJpaEntity> findByStatus(ContentStatus status, Pageable pageable);
    Page<PostJpaEntity> findByStatusAndCategory(ContentStatus status, PostCategory category, Pageable pageable);

    @Query(
            value = """
                    SELECT * FROM posts p
                    WHERE p.status = :status
                      AND NOT EXISTS (
                          SELECT 1 FROM user_blocks ub
                          WHERE (ub.blocker_id = :viewer AND ub.blocked_id = p.author_id)
                             OR (ub.blocker_id = p.author_id AND ub.blocked_id = :viewer)
                      )
                    """,
            countQuery = """
                    SELECT count(*) FROM posts p
                    WHERE p.status = :status
                      AND NOT EXISTS (
                          SELECT 1 FROM user_blocks ub
                          WHERE (ub.blocker_id = :viewer AND ub.blocked_id = p.author_id)
                             OR (ub.blocker_id = p.author_id AND ub.blocked_id = :viewer)
                      )
                    """,
            nativeQuery = true
    )
    Page<PostJpaEntity> findByStatusVisibleTo(@Param("status") String status, @Param("viewer") UUID viewer, Pageable pageable);

    @Query(
            value = """
                    SELECT * FROM posts p
                    WHERE p.status = :status
                      AND p.category = :category
                      AND NOT EXISTS (
                          SELECT 1 FROM user_blocks ub
                          WHERE (ub.blocker_id = :viewer AND ub.blocked_id = p.author_id)
                             OR (ub.blocker_id = p.author_id AND ub.blocked_id = :viewer)
                      )
                    """,
            countQuery = """
                    SELECT count(*) FROM posts p
                    WHERE p.status = :status
                      AND p.category = :category
                      AND NOT EXISTS (
                          SELECT 1 FROM user_blocks ub
                          WHERE (ub.blocker_id = :viewer AND ub.blocked_id = p.author_id)
                             OR (ub.blocker_id = p.author_id AND ub.blocked_id = :viewer)
                      )
                    """,
            nativeQuery = true
    )
    Page<PostJpaEntity> findByStatusAndCategoryVisibleTo(
            @Param("status") String status,
            @Param("category") String category,
            @Param("viewer") UUID viewer,
            Pageable pageable);
}
