package com.ieltspath.content.infrastructure.persistence.entity;

import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;

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
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "combo_unlock_points")
    private Integer comboUnlockPoints;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_skill_prices", joinColumns = @JoinColumn(name = "course_id"))
    @MapKeyColumn(name = "skill")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "unlock_points", nullable = false)
    private Map<Skill, Integer> skillUnlockPoints;
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
