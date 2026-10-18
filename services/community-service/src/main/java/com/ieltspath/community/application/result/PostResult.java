package com.ieltspath.community.application.result;

import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.domain.vo.ReactionType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PostResult(
        UUID id,
        UUID authorId,
        PostCategory category,
        String title,
        String body,
        ContentStatus status,
        Map<ReactionType, Long> reactions,
        Instant createdAt,
        Instant updatedAt,
        java.util.List<String> imageUrls, String videoUrl, java.math.BigDecimal targetBand,
        com.ieltspath.community.domain.vo.PostBackground background,
        PublicProfile author, Map<ReactionType,Long> reactionCounts, ReactionType myReaction,
        long commentCount, boolean bookmarkedByMe, boolean followingAuthor
) {
    public PostResult(UUID id,UUID authorId,PostCategory category,String title,String body,ContentStatus status,
            Map<ReactionType,Long> reactions,Instant createdAt,Instant updatedAt) {
        this(id,authorId,category,title,body,status,reactions,createdAt,updatedAt,java.util.List.of(),null,null,null,
                PublicProfile.anonymous(authorId),reactions,null,0,false,false);
    }
    public PostResult withDetails(PublicProfile profile, com.ieltspath.community.domain.vo.PostInteractions details) {
        return new PostResult(id,authorId,category,title,body,status,details.reactionCounts(),createdAt,updatedAt,
            imageUrls,videoUrl,targetBand,background,profile,details.reactionCounts(),details.myReaction(),details.commentCount(),
            details.bookmarkedByMe(),details.followingAuthor());
    }
    public PostResult {
        reactions = Map.copyOf(reactions);
    }

    public static PostResult from(Post post, Map<ReactionType, Long> reactions) {
        return new PostResult(
                post.getId(), post.getAuthorId(), post.getCategory(), post.getTitle(), post.getBody(),
                post.getStatus(), reactions, post.getCreatedAt(), post.getUpdatedAt(),
                post.getImageUrls(),post.getVideoUrl(),post.getTargetBand(),post.getBackground(),PublicProfile.anonymous(post.getAuthorId()),
                reactions,null,0,false,false
        );
    }
}
