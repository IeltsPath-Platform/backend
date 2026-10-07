package com.ieltspath.community.application.command;

import java.util.UUID;

public record CreateCommentCommand(UUID postId, UUID parentCommentId, String body) {
}
