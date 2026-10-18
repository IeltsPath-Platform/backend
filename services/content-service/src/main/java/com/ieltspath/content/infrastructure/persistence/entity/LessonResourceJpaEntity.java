package com.ieltspath.content.infrastructure.persistence.entity;

import com.ieltspath.content.domain.vo.LessonResourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResourceJpaEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;
    @Column(name = "lesson_id", nullable = false, updatable = false)
    private UUID lessonId;
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, updatable = false, length = 20)
    private LessonResourceType resourceType;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
