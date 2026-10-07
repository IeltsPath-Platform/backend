package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.api.dto.AccessLevel;
import com.ieltspath.library.domain.vo.VideoLevel;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateLearningVideoRequest(
        @NotBlank(message = "youtubeVideoId is required")
        String youtubeVideoId,

        @NotBlank(message = "youtubeUrl is required")
        String youtubeUrl,

        @NotBlank(message = "title is required")
        String title,

        String description,
        String thumbnailUrl,
        Integer durationSeconds,
        UUID topicId,
        VideoLevel level,
        AccessLevel accessLevel
) {}
