package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.api.AccessLevelCompatibility;
import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.application.result.AuthorPackageDetail;
import com.ieltspath.content.application.result.AuthorPackageExtras;
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
 * Author view of a package: every version with its sections, each section's passage and audio, and each question's
 * stem (cut for listing), status and purpose.
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
        UUID topicId,
        UUID courseId,
        String sourceCode,
        Instant createdAt,
        Instant updatedAt,
        List<Version> versions
) {
    public static AuthorPackageResponse from(AuthorPackageDetail detail) {
        return from(detail.pkg(), detail.extras());
    }

    /** A package that was just created has nothing beyond its own fields. */
    public static AuthorPackageResponse from(AuthorPackageResult result) {
        return from(result, AuthorPackageExtras.EMPTY);
    }

    private static AuthorPackageResponse from(AuthorPackageResult result, AuthorPackageExtras extras) {
        return new AuthorPackageResponse(result.id(), result.code(), result.title(), result.packageType(),
                AccessLevelCompatibility.toAccessLevel(result.requiredFeatureKey()), result.status(),
                result.currentPublishedVersionId(), result.lessonId(), result.topicId(), result.courseId(),
                result.sourceCode(), result.createdAt(), result.updatedAt(),
                result.versions().stream().map(version -> Version.from(version, extras)).toList());
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
            return from(result, AuthorPackageExtras.EMPTY);
        }

        static Version from(AuthorVersionResult result, AuthorPackageExtras extras) {
            return new Version(result.id(), result.versionNumber(), result.status(), result.rulesJson(),
                    result.publishedAt(), result.publishAt(), result.lastPublishError(),
                    result.sections().stream().map(section -> Section.from(section, extras)).toList());
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
            String passage,
            Audio audio,
            List<Question> questions
    ) {
        public static Section from(AuthorSectionResult result) {
            return from(result, AuthorPackageExtras.EMPTY);
        }

        static Section from(AuthorSectionResult result, AuthorPackageExtras extras) {
            AuthorPackageExtras.SectionMedia media = extras.media(result.id());
            return new Section(result.id(), result.title(), result.skill(), result.sortOrder(),
                    result.timeLimitSeconds(), result.partNo(), result.coverAssetId(), result.instructions(),
                    media.passage(), Audio.from(media.audio()),
                    result.questions().stream().map(question -> Question.from(question, extras)).toList());
        }
    }

    public record Audio(UUID assetId, String mediaReference, String mediaUrl, Integer durationSeconds,
                        String transcript) {
        public static Audio from(AuthorPackageExtras.Audio audio) {
            return audio == null ? null : new Audio(audio.assetId(), audio.mediaReference(), audio.mediaUrl(),
                    audio.durationSeconds(), audio.transcript());
        }
    }

    public record Question(UUID id, UUID questionVersionId, UUID questionId, int sortOrder,
                           java.math.BigDecimal maxScore, String stem, String status, String purpose) {
        public static Question from(AuthorSectionQuestionResult result) {
            return from(result, AuthorPackageExtras.EMPTY);
        }

        static Question from(AuthorSectionQuestionResult result, AuthorPackageExtras extras) {
            AuthorPackageExtras.QuestionInfo info = extras.question(result.id());
            return new Question(result.id(), result.questionVersionId(), info == null ? null : info.questionId(),
                    result.sortOrder(), result.maxScore(), info == null ? null : info.stem(),
                    info == null ? null : info.status(), info == null ? null : info.purpose());
        }
    }
}
