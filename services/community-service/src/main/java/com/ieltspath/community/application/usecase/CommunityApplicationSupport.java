package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.exception.CommunityForbiddenException;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.vo.ContentStatus;

import java.util.UUID;

final class CommunityApplicationSupport {
    private CommunityApplicationSupport() {
    }

    static Post requireVisiblePost(PostRepository posts, UUID postId, CommunityActor actor) {
        Post post = posts.findById(postId)
                .orElseThrow(() -> new CommunityNotFoundException("Post not found"));
        if (post.getStatus() != ContentStatus.ACTIVE && !actor.administrator()) {
            throw new CommunityNotFoundException("Post not found");
        }
        return post;
    }

    static void requireAdministrator(CommunityActor actor) {
        if (!actor.administrator()) {
            throw new CommunityForbiddenException("Administrator role is required");
        }
    }
}
