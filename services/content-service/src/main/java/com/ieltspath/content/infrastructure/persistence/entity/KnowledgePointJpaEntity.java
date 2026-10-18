package com.ieltspath.content.infrastructure.persistence.entity;

import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgePointJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 50)
    private KnowledgePointKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_type", length = 20)
    private LearningType learningType;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill", length = 50)
    private Skill skill;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", length = 2)
    private CefrLevel cefrLevel;

    @Column(name = "can_do", columnDefinition = "TEXT")
    private String canDo;

    @Column(name = "scope_in", columnDefinition = "TEXT")
    private String scopeIn;

    @Column(name = "scope_out", columnDefinition = "TEXT")
    private String scopeOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ContentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

