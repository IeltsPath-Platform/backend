package com.ieltspath.community.application.command;

import com.ieltspath.community.domain.vo.PostCategory;

public record CreatePostCommand(PostCategory category, String title, String body) {
}
