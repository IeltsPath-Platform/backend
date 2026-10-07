package com.ieltspath.community.application.command;

import com.ieltspath.community.domain.vo.ReactionType;

import java.util.UUID;

public record RemoveReactionCommand(UUID postId, ReactionType type) {
}
