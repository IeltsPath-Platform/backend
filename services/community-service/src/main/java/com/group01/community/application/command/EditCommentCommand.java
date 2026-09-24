package com.group01.community.application.command;

import java.util.UUID;

public record EditCommentCommand(UUID commentId, String body) {
}
