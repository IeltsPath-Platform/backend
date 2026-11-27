package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.exception.CommunityForbiddenException;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.UserBlockRepository;
import com.ieltspath.community.domain.vo.CommunityPage;
import com.ieltspath.community.domain.vo.ContentStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CommunityApplicationSupport {
    private static final UserBlockRepository NO_OP_BLOCKS = new UserBlockRepository() {
        @Override public boolean block(UUID blockerId, UUID blockedId) { return false; }
        @Override public void unblock(UUID blockerId, UUID blockedId) {}
        @Override public boolean isBlockedEitherWay(UUID userA, UUID userB) { return false; }
        @Override public Set<UUID> findBlockedEitherWayUserIds(UUID viewerId) { return Set.of(); }
        @Override public CommunityPage<UUID> findBlockedUsers(UUID blockerId, int page, int size) { return new CommunityPage<>(List.of(), 0L); }
    };

    private CommunityApplicationSupport() {
    }

    static UserBlockRepository noOpBlocks() {
        return NO_OP_BLOCKS;
    }

    static Post requireVisiblePost(PostRepository posts, UUID postId, CommunityActor actor) {
        Post post = posts.findById(postId)
                .orElseThrow(() -> new CommunityNotFoundException("Không tìm thấy bài viết"));
        if (post.getStatus() != ContentStatus.ACTIVE && !actor.administrator()) {
            throw new CommunityNotFoundException("Không tìm thấy bài viết");
        }
        return post;
    }

    static Comment requireVisibleComment(CommentRepository comments, PostRepository posts, UUID commentId, CommunityActor actor) {
        var comment = comments.findById(commentId)
                .orElseThrow(() -> new CommunityNotFoundException("Không tìm thấy bình luận"));
        if (comment.getStatus() != ContentStatus.ACTIVE && !actor.administrator()) {
            throw new CommunityNotFoundException("Không tìm thấy bình luận");
        }
        requireVisiblePost(posts, comment.getPostId(), actor);
        return comment;
    }

    static void requireAdministrator(CommunityActor actor) {
        if (!actor.administrator()) {
            throw new CommunityForbiddenException("Administrator role is required");
        }
    }
}
