package com.group01.community.application.command;

import java.util.UUID;

public record CreateCommentCommand(UUID postId, UUID parentCommentId, String body) {
}
