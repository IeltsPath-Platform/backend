package com.ieltspath.community.application.command;

import com.ieltspath.community.domain.vo.ContentStatus;

import java.util.UUID;

public record ModeratePostCommand(UUID postId, ContentStatus status) {
}
