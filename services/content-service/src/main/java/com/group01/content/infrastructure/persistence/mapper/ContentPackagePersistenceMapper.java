package com.group01.content.infrastructure.persistence.mapper;

import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.entity.ContentSection;
import com.group01.content.domain.entity.SectionQuestion;
import com.group01.content.infrastructure.persistence.entity.ContentPackageJpaEntity;
import com.group01.content.infrastructure.persistence.entity.ContentPackageVersionJpaEntity;
import com.group01.content.infrastructure.persistence.entity.ContentSectionJpaEntity;
import com.group01.content.infrastructure.persistence.entity.SectionQuestionJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContentPackagePersistenceMapper {

    public ContentPackage toDomain(ContentPackageJpaEntity entity) {
        if (entity == null) return null;
        List<ContentPackageVersion> versions = new ArrayList<>();
        if (entity.getVersions() != null) {
            for (ContentPackageVersionJpaEntity v : entity.getVersions()) {
                versions.add(toVersionDomain(v));
            }
        }
        return new ContentPackage(
                entity.getId(),
                entity.getCode(),
                entity.getTitle(),
                entity.getPackageType(),
                entity.getAccessLevel(),
                entity.getStatus(),
                entity.getCurrentPublishedVersionId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                versions
        );
    }

    public ContentPackageJpaEntity toEntity(ContentPackage domain) {
        if (domain == null) return null;
        List<ContentPackageVersionJpaEntity> versionEntities = new ArrayList<>();
        if (domain.getVersions() != null) {
            for (ContentPackageVersion v : domain.getVersions()) {
                versionEntities.add(toVersionEntity(v));
            }
        }
        return ContentPackageJpaEntity.builder()
                .id(domain.getId())
                .code(domain.getCode())
                .title(domain.getTitle())
                .packageType(domain.getPackageType())
                .accessLevel(domain.getAccessLevel())
                .status(domain.getStatus())
                .currentPublishedVersionId(domain.getCurrentPublishedVersionId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .versions(versionEntities)
                .build();
    }

    public ContentPackageVersion toVersionDomain(ContentPackageVersionJpaEntity entity) {
        if (entity == null) return null;
        List<ContentSection> sections = new ArrayList<>();
        if (entity.getSections() != null) {
            for (ContentSectionJpaEntity s : entity.getSections()) {
                sections.add(toSectionDomain(s));
            }
        }
        return new ContentPackageVersion(
                entity.getId(),
                entity.getPackageId(),
                entity.getVersionNumber(),
                entity.getStatus(),
                entity.getRules(),
                entity.getSchemaVersion(),
                entity.getPublishedAt(),
                entity.getPublishedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                sections
        );
    }

    public ContentPackageVersionJpaEntity toVersionEntity(ContentPackageVersion domain) {
        if (domain == null) return null;
        List<ContentSectionJpaEntity> sectionEntities = new ArrayList<>();
        if (domain.getSections() != null) {
            for (ContentSection s : domain.getSections()) {
                sectionEntities.add(toSectionEntity(s));
            }
        }
        return ContentPackageVersionJpaEntity.builder()
                .id(domain.getId())
                .packageId(domain.getPackageId())
                .versionNumber(domain.getVersionNumber())
                .status(domain.getStatus())
                .rules(domain.getRulesJson())
                .schemaVersion(domain.getSchemaVersion())
                .publishedAt(domain.getPublishedAt())
                .publishedBy(domain.getPublishedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .sections(sectionEntities)
                .build();
    }

    public ContentSection toSectionDomain(ContentSectionJpaEntity entity) {
        if (entity == null) return null;
        List<SectionQuestion> questions = new ArrayList<>();
        if (entity.getQuestions() != null) {
            for (SectionQuestionJpaEntity q : entity.getQuestions()) {
                questions.add(toQuestionDomain(q));
            }
        }
        return new ContentSection(
                entity.getId(),
                entity.getPackageVersionId(),
                entity.getTitle(),
                entity.getSkill(),
                entity.getSortOrder(),
                entity.getTimeLimitSeconds(),
                entity.getInstructions(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                questions
        );
    }

    public ContentSectionJpaEntity toSectionEntity(ContentSection domain) {
        if (domain == null) return null;
        List<SectionQuestionJpaEntity> questionEntities = new ArrayList<>();
        if (domain.getQuestions() != null) {
            for (SectionQuestion q : domain.getQuestions()) {
                questionEntities.add(toQuestionEntity(q));
            }
        }
        return ContentSectionJpaEntity.builder()
                .id(domain.getId())
                .packageVersionId(domain.getPackageVersionId())
                .title(domain.getTitle())
                .skill(domain.getSkill())
                .sortOrder(domain.getSortOrder())
                .timeLimitSeconds(domain.getTimeLimitSeconds())
                .instructions(domain.getInstructions())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .questions(questionEntities)
                .build();
    }

    public SectionQuestion toQuestionDomain(SectionQuestionJpaEntity entity) {
        if (entity == null) return null;
        return new SectionQuestion(
                entity.getId(),
                entity.getSectionId(),
                entity.getQuestionVersionId(),
                entity.getSortOrder(),
                entity.getMaxScore(),
                entity.getCreatedAt()
        );
    }

    public SectionQuestionJpaEntity toQuestionEntity(SectionQuestion domain) {
        if (domain == null) return null;
        return SectionQuestionJpaEntity.builder()
                .id(domain.getId())
                .sectionId(domain.getSectionId())
                .questionVersionId(domain.getQuestionVersionId())
                .sortOrder(domain.getSortOrder())
                .maxScore(domain.getMaxScore())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

