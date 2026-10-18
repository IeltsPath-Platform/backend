package com.ieltspath.community.infrastructure.persistence.entity;

import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;
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
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "image_urls", nullable = false, columnDefinition = "jsonb")
    public java.util.List<String> imageUrls = java.util.List.of();
    @Column(name = "video_url") public String videoUrl;
    @Column(name = "target_band", precision = 2, scale = 1) public java.math.BigDecimal targetBand;
    @Version
    public Long version;

    public PostJpaEntity() {
    }
}
