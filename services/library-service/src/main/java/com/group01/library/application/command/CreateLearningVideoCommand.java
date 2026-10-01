package com.group01.library.application.command;

import com.group01.library.domain.vo.VideoLevel;

import java.util.UUID;

public record CreateLearningVideoCommand(
        String youtubeVideoId,
        String youtubeUrl,
        String title,
        String description,
        String thumbnailUrl,
        Integer durationSeconds,
        UUID topicId,
        VideoLevel level,
        String requiredFeatureKey,
        UUID createdBy
) {}
