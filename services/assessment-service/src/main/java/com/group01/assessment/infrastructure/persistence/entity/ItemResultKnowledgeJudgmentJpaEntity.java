package com.group01.assessment.infrastructure.persistence.entity;

import com.group01.assessment.domain.vo.QualitativeJudgment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "item_result_knowledge_judgments")
@Getter
@Setter
@NoArgsConstructor
public class ItemResultKnowledgeJudgmentJpaEntity {
    @EmbeddedId
    private Key id;
    @Enumerated(EnumType.STRING)
    @Column(name = "judgment", nullable = false, length = 20)
    private QualitativeJudgment judgment;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        @Column(name = "item_result_id", nullable = false)
        private UUID itemResultId;
        @Column(name = "knowledge_point_id", nullable = false)
        private UUID knowledgePointId;
    }
}
