package com.ieltspath.community.api.dto.response;

import com.ieltspath.community.application.result.CommentResult;
import com.ieltspath.community.domain.vo.ContentStatus;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID authorId,
        UUID parentCommentId,
        String body,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt,
        com.ieltspath.community.application.result.PublicProfile author,long replyCount
) {
    public static CommentResponse from(CommentResult result) {
        return new CommentResponse(
                result.id(), result.postId(), result.authorId(), result.parentCommentId(), result.body(),
                result.status(), result.createdAt(), result.updatedAt(), result.author(), result.replyCount()
        );
    }
}
