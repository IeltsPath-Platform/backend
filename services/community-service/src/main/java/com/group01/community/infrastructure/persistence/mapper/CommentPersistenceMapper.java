package com.group01.community.infrastructure.persistence.mapper;

import com.group01.community.domain.aggregate.Comment;
import com.group01.community.infrastructure.persistence.entity.CommentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CommentPersistenceMapper {
    public CommentJpaEntity toEntity(Comment comment) {
        CommentJpaEntity entity = new CommentJpaEntity();
        entity.id = comment.getId();
        entity.postId = comment.getPostId();
        entity.authorId = comment.getAuthorId();
        entity.parentCommentId = comment.getParentCommentId();
        entity.body = comment.getBody();
        entity.status = comment.getStatus();
        entity.createdAt = comment.getCreatedAt();
        entity.updatedAt = comment.getUpdatedAt();
        entity.version = comment.getVersion();
        return entity;
    }

    public Comment toDomain(CommentJpaEntity entity) {
        return new Comment(
                entity.id, entity.postId, entity.authorId, entity.parentCommentId,
                entity.body, entity.status, entity.createdAt, entity.updatedAt, entity.version
        );
    }
}
