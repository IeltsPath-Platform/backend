package com.group01.assessment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "error_analysis_items")
@Getter
@Setter
@NoArgsConstructor
public class ErrorAnalysisItemJpaEntity {
    @Id
    private UUID id;
    @Column(name = "item_result_id", nullable = false)
    private UUID itemResultId;
    @Column(name = "knowledge_point_id")
    private UUID knowledgePointId;
    @Column(name = "error_type", nullable = false)
    private String errorType;
    @Column(columnDefinition = "TEXT")
    private String explanation;
}
