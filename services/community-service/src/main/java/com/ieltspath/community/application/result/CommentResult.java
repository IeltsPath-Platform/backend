package com.ieltspath.community.application.result;

import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.CommentInteractions;
import com.ieltspath.community.domain.vo.ReactionType;

import java.time.Instant;
import java.util.UUID;
import java.util.Map;

public record CommentResult(
        UUID id,
        UUID postId,
        UUID authorId,
        UUID parentCommentId,
        String body,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt,
        PublicProfile author, long replyCount,
        Map<ReactionType, Long> reactionCounts, ReactionType myReaction
) {
    public CommentResult(UUID id,UUID postId,UUID authorId,UUID parentCommentId,String body,ContentStatus status,Instant createdAt,Instant updatedAt) {
        this(id,postId,authorId,parentCommentId,body,status,createdAt,updatedAt,PublicProfile.anonymous(authorId),0,Map.of(),null);
    }
    public CommentResult {
        reactionCounts = new CommentInteractions(reactionCounts, myReaction).reactionCounts();
    }

    public CommentResult withDetails(PublicProfile profile, long replies, CommentInteractions interactions) {
        return new CommentResult(id,postId,authorId,parentCommentId,body,status,createdAt,updatedAt,
                profile,replies,interactions.reactionCounts(),interactions.myReaction());
    }
    public static CommentResult from(Comment comment) {
        return new CommentResult(
                comment.getId(), comment.getPostId(), comment.getAuthorId(), comment.getParentCommentId(),
                comment.getBody(), comment.getStatus(), comment.getCreatedAt(), comment.getUpdatedAt()
        );
    }
}
