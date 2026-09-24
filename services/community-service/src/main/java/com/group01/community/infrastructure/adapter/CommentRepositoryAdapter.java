package com.group01.community.infrastructure.adapter;

import com.group01.community.domain.aggregate.Comment;
import com.group01.community.domain.repository.CommentRepository;
import com.group01.community.domain.vo.CommunityPage;
import com.group01.community.domain.vo.ContentStatus;
import com.group01.community.infrastructure.persistence.mapper.CommentPersistenceMapper;
import com.group01.community.infrastructure.persistence.repository.CommentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryAdapter implements CommentRepository {
    private final CommentJpaRepository repository;
    private final CommentPersistenceMapper mapper;

    @Override
    public Comment save(Comment comment) {
        return mapper.toDomain(repository.save(mapper.toEntity(comment)));
    }

    @Override
    public Optional<Comment> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Comment> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public CommunityPage<Comment> findByPostIdAndStatus(UUID postId, ContentStatus status, int page, int size) {
        var sort = Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));
        var result = repository.findByPostIdAndStatus(postId, status, PageRequest.of(page, size, sort));
        return new CommunityPage<>(result.getContent().stream().map(mapper::toDomain).toList(), result.getTotalElements());
    }
}
