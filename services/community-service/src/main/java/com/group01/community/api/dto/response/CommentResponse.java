package com.group01.community.api.dto.response;

import com.group01.community.application.result.CommentResult;
import com.group01.community.domain.vo.ContentStatus;

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
        Instant updatedAt
) {
    public static CommentResponse from(CommentResult result) {
        return new CommentResponse(
                result.id(), result.postId(), result.authorId(), result.parentCommentId(), result.body(),
                result.status(), result.createdAt(), result.updatedAt()
        );
    }
}
