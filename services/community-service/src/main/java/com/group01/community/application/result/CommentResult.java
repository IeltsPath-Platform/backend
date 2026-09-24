package com.group01.community.application.result;

import com.group01.community.domain.aggregate.Comment;
import com.group01.community.domain.vo.ContentStatus;

import java.time.Instant;
import java.util.UUID;

public record CommentResult(
        UUID id,
        UUID postId,
        UUID authorId,
        UUID parentCommentId,
        String body,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResult from(Comment comment) {
        return new CommentResult(
                comment.getId(), comment.getPostId(), comment.getAuthorId(), comment.getParentCommentId(),
                comment.getBody(), comment.getStatus(), comment.getCreatedAt(), comment.getUpdatedAt()
        );
    }
}
