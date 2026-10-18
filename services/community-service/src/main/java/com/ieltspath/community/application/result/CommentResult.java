package com.ieltspath.community.application.result;

import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.vo.ContentStatus;

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
        Instant updatedAt,
        com.ieltspath.community.application.result.PublicProfile author,long replyCount
) {
    public CommentResult(UUID id,UUID postId,UUID authorId,UUID parentCommentId,String body,ContentStatus status,Instant createdAt,Instant updatedAt) {
        this(id,postId,authorId,parentCommentId,body,status,createdAt,updatedAt,PublicProfile.anonymous(authorId),0);
    }
    public CommentResult withDetails(PublicProfile profile,long replies) {return new CommentResult(id,postId,authorId,parentCommentId,body,status,createdAt,updatedAt,profile,replies);}
    public static CommentResult from(Comment comment) {
        return new CommentResult(
                comment.getId(), comment.getPostId(), comment.getAuthorId(), comment.getParentCommentId(),
                comment.getBody(), comment.getStatus(), comment.getCreatedAt(), comment.getUpdatedAt()
        );
    }
}
