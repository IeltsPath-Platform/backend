package com.ieltspath.community.infrastructure.persistence.repository;

import com.ieltspath.community.domain.vo.ReactionType;
import com.ieltspath.community.infrastructure.persistence.entity.CommentReactionId;
import com.ieltspath.community.infrastructure.persistence.entity.CommentReactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CommentReactionJpaRepository extends JpaRepository<CommentReactionJpaEntity, CommentReactionId> {
    @Modifying
    @Query(value = """
            INSERT INTO comment_reactions(comment_id, user_id, reaction_type, created_at)
            VALUES (:commentId, :userId, :type, CURRENT_TIMESTAMP)
            ON CONFLICT(comment_id, user_id) DO UPDATE
            SET reaction_type = EXCLUDED.reaction_type, created_at = EXCLUDED.created_at
            WHERE comment_reactions.reaction_type IS DISTINCT FROM EXCLUDED.reaction_type
            """, nativeQuery = true)
    int insertOrReplace(@Param("commentId") UUID commentId, @Param("userId") UUID userId,
                        @Param("type") String type);

    @Modifying
    @Query("delete from CommentReactionJpaEntity r where r.commentId = :commentId and r.userId = :userId and r.reactionType = :type")
    int remove(@Param("commentId") UUID commentId, @Param("userId") UUID userId,
               @Param("type") ReactionType type);

    @Query("""
            select r.commentId, r.reactionType, count(r)
            from CommentReactionJpaEntity r
            where r.commentId in :commentIds
            group by r.commentId, r.reactionType
            """)
    List<Object[]> countByComments(@Param("commentIds") Set<UUID> commentIds);

    @Query("""
            select r.commentId, r.reactionType from CommentReactionJpaEntity r
            where r.commentId in :commentIds and r.userId = :viewer
            """)
    List<Object[]> findMine(@Param("commentIds") Set<UUID> commentIds, @Param("viewer") UUID viewer);

    @Query(value = """
            SELECT comment_id, reaction_type, count(*), bool_or(user_id = :viewer)
            FROM comment_reactions
            WHERE comment_id IN (:commentIds)
            GROUP BY comment_id, reaction_type
            """, nativeQuery = true)
    List<Object[]> interactions(@Param("commentIds") Set<UUID> commentIds, @Param("viewer") UUID viewer);
}
