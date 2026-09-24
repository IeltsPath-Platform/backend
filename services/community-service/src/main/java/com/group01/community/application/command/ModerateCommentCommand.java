package com.group01.community.application.command;

import com.group01.community.domain.vo.ContentStatus;

import java.util.UUID;

public record ModerateCommentCommand(UUID commentId, ContentStatus status) {
}
