package com.ieltspath.library.application.command;

import com.ieltspath.library.domain.vo.VideoLevel;

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
