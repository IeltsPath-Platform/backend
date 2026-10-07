package com.ieltspath.community.domain.repository;

import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.vo.CommunityPage;
import com.ieltspath.community.domain.vo.ContentStatus;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository {
    Comment save(Comment comment);

    Optional<Comment> findById(UUID id);

    Optional<Comment> findByIdForUpdate(UUID id);

    CommunityPage<Comment> findByPostIdAndStatus(UUID postId, ContentStatus status, int page, int size);
}
