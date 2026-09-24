package com.group01.community.api.dto.response;

import com.group01.community.application.result.PostResult;
import com.group01.community.domain.vo.ContentStatus;
import com.group01.community.domain.vo.PostCategory;
import com.group01.community.domain.vo.ReactionType;

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
        Instant updatedAt
) {
    public static PostResponse from(PostResult result) {
        return new PostResponse(
                result.id(), result.authorId(), result.category(), result.title(), result.body(),
                result.status(), result.reactions(), result.createdAt(), result.updatedAt()
        );
    }
}
