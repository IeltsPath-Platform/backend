package com.group01.content.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class QuestionKnowledgePointId implements Serializable {

    @Column(name = "question_version_id", nullable = false)
    private UUID questionVersionId;

    @Column(name = "knowledge_point_id", nullable = false)
    private UUID knowledgePointId;
}

