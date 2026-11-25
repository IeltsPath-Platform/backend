package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PlacementPool;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class AuthorPackageRequests {

    private AuthorPackageRequests() {
    }

    /**
     * {@code topicId} is for {@code TOPIC_TEST}, {@code courseId} for {@code COURSE_TEST}, {@code lessonId} for a
     * {@code PRACTICE_SET} and {@code sourceCode} for a {@code PRACTICE_TEST}; the use case checks which fit.
     */
    public record CreatePackage(
            @Size(max = 100) String code,
            @NotBlank @Size(max = 255) String title,
            @NotNull PackageType packageType,
            AccessLevel accessLevel,
            UUID lessonId,
            @Size(max = 40) String sourceCode,
            UUID topicId,
            UUID courseId,
            PlacementPool placementPool) {
        @AssertTrue(message = "Nhóm đề xếp lớp (placementPool) chỉ bắt buộc và chỉ dùng cho đề xếp lớp")
        public boolean isPlacementPoolValid() {
            return packageType == null || (packageType == PackageType.PLACEMENT_TEST) == (placementPool != null);
        }
    }

    /** A null field is left unchanged; a blank {@code sourceCode} takes a practice test out of the library. */
    public record UpdatePackageInfo(
            @Size(max = 255) String title,
            AccessLevel accessLevel,
            @Size(max = 40) String sourceCode,
            PlacementPool placementPool) {
    }

    /** {@code rulesJson} defaults to {@code "{}"} when omitted; a non-JSON value is refused by the use case. */
    public record CreateDraftVersion(@Size(max = 20000) String rulesJson) {
    }

    public record ReorderSections(@NotNull List<UUID> sectionIds) {
    }

    public record AddSectionQuestion(@NotNull UUID questionVersionId, java.math.BigDecimal maxScore) {
    }

    public record ReorderSectionQuestions(@NotNull List<UUID> sectionQuestionIds) {
    }
}
