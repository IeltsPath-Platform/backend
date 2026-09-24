package com.group01.community.infrastructure.persistence.entity;

import com.group01.community.domain.vo.ContentStatus;
import com.group01.community.domain.vo.PostCategory;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class PostJpaEntity {
    @Id
    public UUID id;
    @Column(name = "author_id", nullable = false)
    public UUID authorId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public PostCategory category;
    public String title;
    @Column(nullable = false, columnDefinition = "text")
    public String body;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ContentStatus status;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
    @Version
    public Long version;

    public PostJpaEntity() {
    }
}
