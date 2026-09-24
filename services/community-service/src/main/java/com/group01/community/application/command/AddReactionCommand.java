package com.group01.community.application.command;

import com.group01.community.domain.vo.ReactionType;

import java.util.UUID;

public record AddReactionCommand(UUID postId, ReactionType type) {
}
