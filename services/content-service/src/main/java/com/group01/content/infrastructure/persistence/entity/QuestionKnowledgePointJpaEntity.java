package com.group01.content.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "question_knowledge_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionKnowledgePointJpaEntity {

    @EmbeddedId
    private QuestionKnowledgePointId id;

    @Column(name = "weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;
}

