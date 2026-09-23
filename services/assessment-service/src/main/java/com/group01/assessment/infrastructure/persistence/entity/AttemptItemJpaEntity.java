package com.group01.assessment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "attempt_items", uniqueConstraints = @UniqueConstraint(name = "uk_attempt_item_order", columnNames = {"attempt_section_id", "sort_order"}))
@Getter @Setter @NoArgsConstructor
public class AttemptItemJpaEntity {
    @Id private UUID id;
    @Column(name = "attempt_section_id", nullable = false) private UUID attemptSectionId;
    @Column(name = "question_version_id", nullable = false) private UUID questionVersionId;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "question_snapshot", columnDefinition = "JSONB", nullable = false) private String questionSnapshot;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "answer_snapshot", columnDefinition = "JSONB") private String answerSnapshot;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "knowledge_snapshot", columnDefinition = "JSONB") private String knowledgeSnapshot;
}
