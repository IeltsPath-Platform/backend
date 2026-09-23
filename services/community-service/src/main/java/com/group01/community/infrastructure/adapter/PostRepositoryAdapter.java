package com.group01.community.infrastructure.adapter;

import com.group01.community.domain.aggregate.Post;
import com.group01.community.domain.repository.PostRepository;
import com.group01.community.domain.repository.PageQuery;
import com.group01.community.domain.repository.PageResult;
import com.group01.community.domain.vo.ContentStatus;
import com.group01.community.infrastructure.persistence.PostJpaEntity;
import com.group01.community.infrastructure.persistence.PostJpaRepository;
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
public class PostRepositoryAdapter implements PostRepository {
    private final PostJpaRepository repository;

    public Post save(Post p) {
        return toDomain(repository.save(toEntity(p)));
    }

    public Optional<Post> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    public PageResult<Post> findByStatus(ContentStatus status, PageQuery query) {
        Page<Post> p = repository.findByStatus(status, pageable(query)).map(this::toDomain);
        return new PageResult<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    private Pageable pageable(PageQuery q) {
        Sort.Direction d = q.descending() ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(q.page(), q.size(), Sort.by(d, q.sortProperty()).and(Sort.by(d, "id")));
    }

    private PostJpaEntity toEntity(Post p) {
        PostJpaEntity e = new PostJpaEntity();
        e.id = p.getId();
        e.authorId = p.getAuthorId();
        e.category = p.getCategory();
        e.title = p.getTitle();
        e.body = p.getBody();
        e.status = p.getStatus();
        e.createdAt = p.getCreatedAt();
        e.updatedAt = p.getUpdatedAt();
        e.version = p.getVersion();
        return e;
    }

    private Post toDomain(PostJpaEntity e) {
        return new Post(e.id, e.authorId, e.category, e.title, e.body, e.status, e.createdAt, e.updatedAt, e.version);
    }
}
