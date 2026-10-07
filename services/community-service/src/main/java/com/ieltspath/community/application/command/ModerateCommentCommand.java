package com.ieltspath.community.application.command;

import com.ieltspath.community.domain.vo.ContentStatus;

import java.util.UUID;

public record ModerateCommentCommand(UUID commentId, ContentStatus status) {
}
