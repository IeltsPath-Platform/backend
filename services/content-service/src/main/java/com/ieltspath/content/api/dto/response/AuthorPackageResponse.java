package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.api.AccessLevelCompatibility;
import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.application.result.AuthorPackageResult;
import com.ieltspath.content.application.result.AuthorSectionQuestionResult;
import com.ieltspath.content.application.result.AuthorSectionResult;
import com.ieltspath.content.application.result.AuthorVersionResult;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.Skill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Author view of a package: every version with its sections. Passage, audio and question stems are not part of this
 * response yet; see the package authoring contract.
 */
public record AuthorPackageResponse(
        UUID id,
        String code,
        String title,
        PackageType packageType,
        AccessLevel accessLevel,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        UUID lessonId,
        UUID courseId,
        String sourceCode,
        Instant createdAt,
        Instant updatedAt,
        List<Version> versions
) {
    public static AuthorPackageResponse from(AuthorPackageResult result) {
        return new AuthorPackageResponse(result.id(), result.code(), result.title(), result.packageType(),
                AccessLevelCompatibility.toAccessLevel(result.requiredFeatureKey()), result.status(),
                result.currentPublishedVersionId(), result.lessonId(), result.courseId(), result.sourceCode(),
                result.createdAt(), result.updatedAt(),
                result.versions().stream().map(Version::from).toList());
    }

    public record Version(
            UUID id,
            int versionNumber,
            PublicationStatus status,
            String rulesJson,
            Instant publishedAt,
            Instant publishAt,
            String lastPublishError,
            List<Section> sections
    ) {
        public static Version from(AuthorVersionResult result) {
            return new Version(result.id(), result.versionNumber(), result.status(), result.rulesJson(),
                    result.publishedAt(), result.publishAt(), result.lastPublishError(),
                    result.sections().stream().map(Section::from).toList());
        }
    }

    public record Section(
            UUID id,
            String title,
            Skill skill,
            int sortOrder,
            Integer timeLimitSeconds,
            Integer partNo,
            UUID coverAssetId,
            String instructions,
            List<Question> questions
    ) {
        public static Section from(AuthorSectionResult result) {
            return new Section(result.id(), result.title(), result.skill(), result.sortOrder(),
                    result.timeLimitSeconds(), result.partNo(), result.coverAssetId(), result.instructions(),
                    result.questions().stream().map(Question::from).toList());
        }
    }

    public record Question(UUID id, UUID questionVersionId, int sortOrder, java.math.BigDecimal maxScore) {
        public static Question from(AuthorSectionQuestionResult result) {
            return new Question(result.id(), result.questionVersionId(), result.sortOrder(), result.maxScore());
        }
    }
}
