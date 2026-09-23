package com.group01.community.application;

import com.group01.commonsecurity.currentuser.CurrentUser;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.commonsecurity.role.CanonicalRoles;
import com.group01.community.domain.aggregate.*;
import com.group01.community.domain.exception.*;
import com.group01.community.domain.repository.*;
import com.group01.community.domain.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityService {
    private final PostRepository posts;
    private final CommentRepository comments;
    private final ReactionRepository reactions;
    private final CurrentUserProvider currentUser;

    @Transactional
    public Post createPost(PostCategory category, String title, String body) {
        return posts.save(Post.create(currentUser.requireUserId(), category, title, body));
    }

    @Transactional(readOnly = true)
    public Post getPost(UUID id) {
        Post p = requirePost(id);
        if (p.getStatus() != ContentStatus.ACTIVE && !isAdmin()) {
            throw new CommunityNotFoundException("Post not found");
        }
        return p;
    }

    @Transactional(readOnly = true)
    public PageResult<Post> listPosts(int page, int size) {
        validatePage(page, size);
        return posts.findByStatus(ContentStatus.ACTIVE, new PageQuery(page, size, "createdAt", true));
    }

    @Transactional
    public Post editPost(UUID id, PostCategory category, String title, String body) {
        Post p = requirePost(id);
        p.edit(currentUser.requireUserId(), category, title, body);
        return posts.save(p);
    }

    @Transactional
    public void deletePost(UUID id) {
        Post p = requirePost(id);
        p.delete(currentUser.requireUserId());
        posts.save(p);
    }

    @Transactional
    public Post moderatePost(UUID id, ContentStatus status) {
        requireAdmin();
        Post p = requirePost(id);
        p.moderate(status);
        return posts.save(p);
    }

    @Transactional
    public Comment createComment(UUID postId, UUID parentId, String body) {
        getPost(postId);
        if (parentId != null) {
            Comment parent = comments.findByIdForUpdate(parentId)
                    .orElseThrow(() -> new CommunityNotFoundException("Comment not found"));
            if (!parent.getPostId().equals(postId) || parent.getStatus() != ContentStatus.ACTIVE) {
                throw new CommunityException("Parent comment must be active and belong to the same post");
            }
        }
        return comments.save(Comment.create(postId, currentUser.requireUserId(), parentId, body));
    }

    @Transactional(readOnly = true)
    public PageResult<Comment> listComments(UUID postId, int page, int size) {
        getPost(postId);
        validatePage(page, size);
        return comments.findByPostIdAndStatus(
                postId,
                ContentStatus.ACTIVE,
                new PageQuery(page, size, "createdAt", false)
        );
    }

    @Transactional
    public Comment editComment(UUID id, String body) {
        Comment c = requireComment(id);
        c.edit(currentUser.requireUserId(), body);
        return comments.save(c);
    }

    @Transactional
    public void deleteComment(UUID id) {
        Comment c = requireComment(id);
        c.delete(currentUser.requireUserId());
        comments.save(c);
    }

    @Transactional
    public Comment moderateComment(UUID id, ContentStatus status) {
        requireAdmin();
        Comment c = requireComment(id);
        c.moderate(status);
        return comments.save(c);
    }

    @Transactional
    public void addReaction(UUID postId, ReactionType type) {
        getPost(postId);
        reactions.add(postId, currentUser.requireUserId(), type);
    }

    @Transactional
    public void removeReaction(UUID postId, ReactionType type) {
        reactions.remove(postId, currentUser.requireUserId(), type);
    }

    @Transactional(readOnly = true)
    public java.util.Map<ReactionType, Long> reactionCounts(UUID postId) {
        return reactions.counts(postId);
    }

    @Transactional(readOnly = true)
    public java.util.Map<UUID, java.util.Map<ReactionType, Long>> reactionCounts(
            java.util.Set<UUID> postIds
    ) {
        return reactions.counts(postIds);
    }

    private Post requirePost(UUID id) {
        return posts.findById(id).orElseThrow(() -> new CommunityNotFoundException("Post not found"));
    }

    private Comment requireComment(UUID id) {
        return comments.findById(id).orElseThrow(() -> new CommunityNotFoundException("Comment not found"));
    }

    private boolean isAdmin() {
        return currentUser.requireCurrentUser().hasRole(CanonicalRoles.ADMIN);
    }

    private void requireAdmin() {
        CurrentUser user = currentUser.requireCurrentUser();
        if (!user.hasRole(CanonicalRoles.ADMIN)) {
            throw new CommunityForbiddenException("Administrator role is required");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new CommunityException("Page must be non-negative");
        }
        if (size < 1 || size > 100) {
            throw new CommunityException("Size must be between 1 and 100");
        }
    }
}
