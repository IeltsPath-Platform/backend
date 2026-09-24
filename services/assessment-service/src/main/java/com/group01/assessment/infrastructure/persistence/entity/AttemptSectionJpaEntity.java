package com.group01.assessment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "attempt_sections", uniqueConstraints = @UniqueConstraint(name = "uk_attempt_section_order", columnNames = {
        "attempt_id", "sort_order"}))
@Getter
@Setter
@NoArgsConstructor
public class AttemptSectionJpaEntity {
    @Id
    private UUID id;
    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;
    @Column(name = "content_section_id", nullable = false)
    private UUID contentSectionId;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_snapshot", columnDefinition = "JSONB", nullable = false)
    private String snapshot;
}
