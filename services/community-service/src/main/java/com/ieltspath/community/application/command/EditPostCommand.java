package com.ieltspath.community.application.command;

import com.ieltspath.community.domain.vo.PostCategory;

import java.util.UUID;

public record EditPostCommand(UUID postId, PostCategory category, String title, String body) {
}
