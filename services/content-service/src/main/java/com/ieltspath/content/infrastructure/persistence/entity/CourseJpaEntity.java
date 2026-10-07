package com.ieltspath.content.infrastructure.persistence.entity;

import com.ieltspath.content.domain.vo.ContentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseJpaEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(name = "band_level", nullable = false, unique = true, precision = 2, scale = 1)
    private BigDecimal bandLevel;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
