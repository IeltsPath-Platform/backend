package com.ieltspath.community.api.dto.response;

import com.ieltspath.community.application.result.PostResult;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.domain.vo.ReactionType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID authorId,
        PostCategory category,
        String title,
        String body,
        ContentStatus status,
        Map<ReactionType, Long> reactions,
        Instant createdAt,
        Instant updatedAt,
        java.util.List<String> imageUrls,String videoUrl,java.math.BigDecimal targetBand,
        com.ieltspath.community.application.result.PublicProfile author,Map<ReactionType,Long> reactionCounts,
        ReactionType myReaction,long commentCount,boolean bookmarkedByMe,boolean followingAuthor
) {
    public static PostResponse from(PostResult result) {
        return new PostResponse(
                result.id(), result.authorId(), result.category(), result.title(), result.body(),
                result.status(), result.reactions(), result.createdAt(), result.updatedAt(),
                result.imageUrls(),result.videoUrl(),result.targetBand(),result.author(),result.reactionCounts(),
                result.myReaction(),result.commentCount(),result.bookmarkedByMe(),result.followingAuthor()
        );
    }
}
