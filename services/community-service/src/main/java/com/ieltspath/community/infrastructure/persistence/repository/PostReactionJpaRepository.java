package com.ieltspath.community.infrastructure.persistence.repository;

import com.ieltspath.community.infrastructure.persistence.entity.PostReactionId;
import com.ieltspath.community.infrastructure.persistence.entity.PostReactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PostReactionJpaRepository extends JpaRepository<PostReactionJpaEntity, PostReactionId> {
    @Modifying
    @Query(
            value = """
                    INSERT INTO post_reactions(post_id, user_id, reaction_type, created_at)
                    VALUES (:postId, :userId, :type, CURRENT_TIMESTAMP)
                    ON CONFLICT(post_id,user_id) DO UPDATE
                    SET reaction_type=EXCLUDED.reaction_type, created_at=EXCLUDED.created_at
                    WHERE post_reactions.reaction_type IS DISTINCT FROM EXCLUDED.reaction_type
                    """,
            nativeQuery = true
    )
    int insertOrReplace(@Param("postId") UUID postId, @Param("userId") UUID userId, @Param("type") String type);

    @Modifying
    @Query("delete from PostReactionJpaEntity r where r.postId = :postId and r.userId = :userId and r.reactionType = :type")
    int remove(@Param("postId") UUID postId, @Param("userId") UUID userId,
               @Param("type") com.ieltspath.community.domain.vo.ReactionType type);

    @Query("""
            select r.reactionType, count(r)
            from PostReactionJpaEntity r
            where r.postId = :postId
            group by r.reactionType
            """)
    List<Object[]> countByPost(@Param("postId") UUID postId);

    @Query("""
            select r.postId, r.reactionType, count(r)
            from PostReactionJpaEntity r
            where r.postId in :postIds
            group by r.postId, r.reactionType
            """)
    List<Object[]> countByPosts(@Param("postIds") Set<UUID> postIds);
}
