package com.group01.assessment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "attempt_item_knowledge_points")
@Getter
@Setter
@NoArgsConstructor
public class AttemptItemKnowledgePointJpaEntity {
    @EmbeddedId
    private Key id;
    @Column(name = "weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        @Column(name = "attempt_item_id", nullable = false)
        private UUID attemptItemId;
        @Column(name = "knowledge_point_id", nullable = false)
        private UUID knowledgePointId;
    }
}
