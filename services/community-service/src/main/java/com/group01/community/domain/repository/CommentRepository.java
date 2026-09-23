package com.group01.community.domain.repository;

import com.group01.community.domain.aggregate.Comment;
import com.group01.community.domain.vo.ContentStatus;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository {
    Comment save(Comment comment);

    Optional<Comment> findById(UUID id);

    Optional<Comment> findByIdForUpdate(UUID id);

    PageResult<Comment> findByPostIdAndStatus(UUID postId, ContentStatus status, PageQuery query);
}
