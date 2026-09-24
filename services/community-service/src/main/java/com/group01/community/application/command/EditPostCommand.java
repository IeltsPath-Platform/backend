package com.group01.community.application.command;

import com.group01.community.domain.vo.PostCategory;

import java.util.UUID;

public record EditPostCommand(UUID postId, PostCategory category, String title, String body) {
}
