package com.group01.community.application.command;

import java.util.UUID;

public record DeleteCommentCommand(UUID commentId) {
}
