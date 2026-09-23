package com.group01.community.api.dto;

import com.group01.community.domain.vo.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CommunityDtos {
    private CommunityDtos() {
    }

    public record PostRequest(@NotNull PostCategory category, @Size(max = 200) String title,
                              @NotBlank @Size(max = 20000) String body) {
    }

    public record CommentRequest(UUID parentCommentId, @NotBlank @Size(max = 5000) String body) {
    }

    public record UpdateCommentRequest(@NotBlank @Size(max = 5000) String body) {
    }

    public record ModerationRequest(@NotNull ContentStatus status) {
    }

    public record PostResponse(UUID id, UUID authorId, PostCategory category, String title, String body,
                               ContentStatus status, java.util.Map<ReactionType, Long> reactions, Instant createdAt,
                               Instant updatedAt) {
    }

    public record CommentResponse(UUID id, UUID postId, UUID authorId, UUID parentCommentId, String body,
                                  ContentStatus status, Instant createdAt, Instant updatedAt) {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages,
                                  String sort) {
    }
}
