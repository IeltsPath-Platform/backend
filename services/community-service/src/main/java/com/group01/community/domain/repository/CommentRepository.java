package com.group01.community.domain.repository;

import com.group01.community.domain.aggregate.Comment;
import com.group01.community.domain.vo.CommunityPage;
import com.group01.community.domain.vo.ContentStatus;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository {
    Comment save(Comment comment);

    Optional<Comment> findById(UUID id);

    Optional<Comment> findByIdForUpdate(UUID id);

    CommunityPage<Comment> findByPostIdAndStatus(UUID postId, ContentStatus status, int page, int size);
}
