package com.group01.community.infrastructure.adapter;

import com.group01.community.domain.aggregate.Post;
import com.group01.community.domain.repository.PostRepository;
import com.group01.community.domain.vo.CommunityPage;
import com.group01.community.domain.vo.ContentStatus;
import com.group01.community.infrastructure.persistence.mapper.PostPersistenceMapper;
import com.group01.community.infrastructure.persistence.repository.PostJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostRepositoryAdapter implements PostRepository {
    private final PostJpaRepository repository;
    private final PostPersistenceMapper mapper;

    @Override
    public Post save(Post post) {
        return mapper.toDomain(repository.save(mapper.toEntity(post)));
    }

    @Override
    public Optional<Post> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public CommunityPage<Post> findByStatus(ContentStatus status, int page, int size) {
        var sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        var result = repository.findByStatus(status, PageRequest.of(page, size, sort));
        return new CommunityPage<>(result.getContent().stream().map(mapper::toDomain).toList(), result.getTotalElements());
    }
}
