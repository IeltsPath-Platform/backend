package com.group01.content.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.entity.QuestionKnowledgePoint;
import com.group01.content.domain.entity.QuestionVersion;
import com.group01.content.domain.vo.QuestionOptionPayload;
import com.group01.content.infrastructure.persistence.entity.QuestionJpaEntity;
import com.group01.content.infrastructure.persistence.entity.QuestionKnowledgePointId;
import com.group01.content.infrastructure.persistence.entity.QuestionKnowledgePointJpaEntity;
import com.group01.content.infrastructure.persistence.entity.QuestionVersionJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class QuestionPersistenceMapper {

    private final ObjectMapper objectMapper;

    public QuestionPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Question toDomain(QuestionJpaEntity entity) {
        if (entity == null) return null;
        List<QuestionVersion> versions = new ArrayList<>();
        if (entity.getVersions() != null) {
            for (QuestionVersionJpaEntity v : entity.getVersions()) {
                versions.add(toVersionDomain(v));
            }
        }
        return new Question(
                entity.getId(),
                entity.getQuestionType(),
                entity.getSkill(),
                entity.getAccessLevel(),
                entity.getStatus(),
                entity.getCurrentPublishedVersionId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                versions
        );
    }

    public QuestionJpaEntity toEntity(Question domain) {
        if (domain == null) return null;
        List<QuestionVersionJpaEntity> versionEntities = new ArrayList<>();
        if (domain.getVersions() != null) {
            for (QuestionVersion v : domain.getVersions()) {
                versionEntities.add(toVersionEntity(v));
            }
        }
        return QuestionJpaEntity.builder()
                .id(domain.getId())
                .questionType(domain.getQuestionType())
                .skill(domain.getSkill())
                .accessLevel(domain.getAccessLevel())
                .status(domain.getStatus())
                .currentPublishedVersionId(domain.getCurrentPublishedVersionId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .versions(versionEntities)
                .build();
    }

    public QuestionVersion toVersionDomain(QuestionVersionJpaEntity entity) {
        if (entity == null) return null;
        List<QuestionOptionPayload> options = new ArrayList<>();
        if (entity.getOptions() != null && !entity.getOptions().isBlank()) {
            try {
                options = objectMapper.readValue(entity.getOptions(), new TypeReference<List<QuestionOptionPayload>>() {});
            } catch (Exception ignored) {}
        }
        List<QuestionKnowledgePoint> kps = new ArrayList<>();
        if (entity.getKnowledgePoints() != null) {
            for (QuestionKnowledgePointJpaEntity kp : entity.getKnowledgePoints()) {
                kps.add(new QuestionKnowledgePoint(kp.getId().getQuestionVersionId(), kp.getId().getKnowledgePointId(), kp.getWeight()));
            }
        }
        return new QuestionVersion(
                entity.getId(),
                entity.getQuestionId(),
                entity.getVersionNumber(),
                entity.getStem(),
                options,
                entity.getAnswerSpec(),
                entity.getSchemaVersion(),
                entity.getExplanation(),
                entity.getDifficulty(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                kps
        );
    }

    public QuestionVersionJpaEntity toVersionEntity(QuestionVersion domain) {
        if (domain == null) return null;
        String optionsJson = "[]";
        try {
            optionsJson = objectMapper.writeValueAsString(domain.getOptions());
        } catch (Exception ignored) {}

        List<QuestionKnowledgePointJpaEntity> kpEntities = new ArrayList<>();
        if (domain.getKnowledgePoints() != null) {
            for (QuestionKnowledgePoint kp : domain.getKnowledgePoints()) {
                kpEntities.add(QuestionKnowledgePointJpaEntity.builder()
                        .id(new QuestionKnowledgePointId(domain.getId(), kp.getKnowledgePointId()))
                        .weight(kp.getWeight())
                        .build());
            }
        }

        return QuestionVersionJpaEntity.builder()
                .id(domain.getId())
                .questionId(domain.getQuestionId())
                .versionNumber(domain.getVersionNumber())
                .stem(domain.getStem())
                .options(optionsJson)
                .answerSpec(domain.getAnswerSpecJson())
                .schemaVersion(domain.getSchemaVersion())
                .explanation(domain.getExplanation())
                .difficulty(domain.getDifficulty())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .knowledgePoints(kpEntities)
                .build();
    }
}

