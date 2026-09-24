package com.group01.community.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CommentRequest(UUID parentCommentId, @NotBlank @Size(max = 5000) String body) {
}
