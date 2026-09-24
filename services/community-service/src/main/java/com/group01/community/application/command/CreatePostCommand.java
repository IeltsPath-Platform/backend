package com.group01.community.application.command;

import com.group01.community.domain.vo.PostCategory;

public record CreatePostCommand(PostCategory category, String title, String body) {
}
