package com.ieltspath.community.api.dto.response;

import com.ieltspath.community.application.result.CommentResult;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.ReactionType;

import java.time.Instant;
import java.util.UUID;
import java.util.Map;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID authorId,
        UUID parentCommentId,
        String body,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt,
        com.ieltspath.community.application.result.PublicProfile author,long replyCount,
        Map<ReactionType, Long> reactionCounts, ReactionType myReaction
) {
    public static CommentResponse from(CommentResult result) {
        return new CommentResponse(
                result.id(), result.postId(), result.authorId(), result.parentCommentId(), result.body(),
                result.status(), result.createdAt(), result.updatedAt(), result.author(), result.replyCount(),
                result.reactionCounts(), result.myReaction()
        );
    }
}
