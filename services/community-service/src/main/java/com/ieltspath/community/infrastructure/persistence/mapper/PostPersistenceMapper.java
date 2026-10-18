package com.ieltspath.community.infrastructure.persistence.mapper;

import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.infrastructure.persistence.entity.PostJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PostPersistenceMapper {
    public PostJpaEntity toEntity(Post post) {
        PostJpaEntity entity = new PostJpaEntity();
        entity.id = post.getId();
        entity.authorId = post.getAuthorId();
        entity.category = post.getCategory();
        entity.title = post.getTitle();
        entity.body = post.getBody();
        entity.status = post.getStatus();
        entity.createdAt = post.getCreatedAt();
        entity.updatedAt = post.getUpdatedAt();
        entity.version = post.getVersion();
        entity.imageUrls = post.getImageUrls(); entity.videoUrl = post.getVideoUrl(); entity.targetBand = post.getTargetBand();
        return entity;
    }

    public Post toDomain(PostJpaEntity entity) {
        Post post = new Post(
                entity.id, entity.authorId, entity.category, entity.title, entity.body, entity.status,
                entity.createdAt, entity.updatedAt, entity.version
        );
        post.setMedia(entity.imageUrls, entity.videoUrl, entity.targetBand);
        return post;
    }
}
