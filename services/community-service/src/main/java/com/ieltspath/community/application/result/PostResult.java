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
        Instant updatedAt
) {
    public PostResult {
        reactions = Map.copyOf(reactions);
    }

    public static PostResult from(Post post, Map<ReactionType, Long> reactions) {
        return new PostResult(
                post.getId(), post.getAuthorId(), post.getCategory(), post.getTitle(), post.getBody(),
                post.getStatus(), reactions, post.getCreatedAt(), post.getUpdatedAt()
        );
    }
}
