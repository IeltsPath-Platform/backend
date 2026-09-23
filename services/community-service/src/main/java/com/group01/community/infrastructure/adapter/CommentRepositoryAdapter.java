package com.group01.community.infrastructure.adapter;

import com.group01.community.domain.aggregate.Comment;
import com.group01.community.domain.repository.CommentRepository;
import com.group01.community.domain.repository.PageQuery;
import com.group01.community.domain.repository.PageResult;
import com.group01.community.domain.vo.ContentStatus;
import com.group01.community.infrastructure.persistence.CommentJpaEntity;
import com.group01.community.infrastructure.persistence.CommentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryAdapter implements CommentRepository {
    private final CommentJpaRepository repository;

    public Comment save(Comment c) {
        return toDomain(repository.save(toEntity(c)));
    }

    public Optional<Comment> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    public Optional<Comment> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(this::toDomain);
    }

    public PageResult<Comment> findByPostIdAndStatus(UUID postId, ContentStatus status, PageQuery query) {
        Page<Comment> p = repository.findByPostIdAndStatus(postId, status, pageable(query)).map(this::toDomain);
        return new PageResult<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    private Pageable pageable(PageQuery q) {
        Sort.Direction d = q.descending() ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(q.page(), q.size(), Sort.by(d, q.sortProperty()).and(Sort.by(d, "id")));
    }

    private CommentJpaEntity toEntity(Comment c) {
        CommentJpaEntity e = new CommentJpaEntity();
        e.id = c.getId();
        e.postId = c.getPostId();
        e.authorId = c.getAuthorId();
        e.parentCommentId = c.getParentCommentId();
        e.body = c.getBody();
        e.status = c.getStatus();
        e.createdAt = c.getCreatedAt();
        e.updatedAt = c.getUpdatedAt();
        e.version = c.getVersion();
        return e;
    }

    private Comment toDomain(CommentJpaEntity e) {
        return new Comment(
                e.id,
                e.postId,
                e.authorId,
                e.parentCommentId,
                e.body,
                e.status,
                e.createdAt,
                e.updatedAt,
                e.version
        );
    }
}
