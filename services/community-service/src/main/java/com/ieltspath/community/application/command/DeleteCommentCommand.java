package com.ieltspath.community.application.command;

import java.util.UUID;

public record DeleteCommentCommand(UUID commentId) {
}
