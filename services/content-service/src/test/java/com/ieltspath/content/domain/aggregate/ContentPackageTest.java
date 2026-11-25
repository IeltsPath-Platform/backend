package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.exception.InvalidPackageLessonException;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentPackageTest {

    @Test
    @DisplayName("Should create content package in DRAFT status")
    void shouldCreatePackageInDraftStatus() {
        ContentPackage pkg = ContentPackage.create("PKG_01", "Cambridge 18 Test 1", PackageType.MOCK_TEST, null);

        assertThat(pkg.getId()).isNotNull();
        assertThat(pkg.getCode()).isEqualTo("PKG_01");
        assertThat(pkg.getTitle()).isEqualTo("Cambridge 18 Test 1");
        assertThat(pkg.getPackageType()).isEqualTo(PackageType.MOCK_TEST);
        assertThat(pkg.getRequiredFeatureKey()).isNull();
        assertThat(pkg.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(pkg.getCurrentPublishedVersionId()).isNull();
    }

    @Test
    @DisplayName("Should add version and publish version correctly")
    void shouldAddAndPublishVersion() {
        ContentPackage pkg = ContentPackage.create("PKG_01", "Cambridge 18 Test 1", PackageType.MOCK_TEST, null);
        ContentPackageVersion v1 = ContentPackageVersion.create(pkg.getId(), 1, "{}");

        pkg.addVersion(v1);
        assertThat(pkg.getVersions()).hasSize(1);

        pkg.publishVersion(v1.getId());
        assertThat(pkg.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(pkg.getCurrentPublishedVersionId()).isEqualTo(v1.getId());
    }

    @Test
    @DisplayName("A placement test changes pool only before its first publication")
    void placementPoolChangesOnlyBeforeFirstPublication() {
        ContentPackage pkg = ContentPackage.create("PL_01", "Đề đầu vào", PackageType.PLACEMENT_TEST, null, null, null,
                com.ieltspath.content.domain.vo.PlacementPool.BAND_0_3);
        pkg.changePlacementPool(com.ieltspath.content.domain.vo.PlacementPool.BAND_3_5);
        assertThat(pkg.getPlacementPool()).isEqualTo(com.ieltspath.content.domain.vo.PlacementPool.BAND_3_5);

        ContentPackageVersion v1 = ContentPackageVersion.create(pkg.getId(), 1, "{}");
        pkg.addVersion(v1);
        pkg.publishVersion(v1.getId());

        assertThatThrownBy(() -> pkg.changePlacementPool(com.ieltspath.content.domain.vo.PlacementPool.BAND_5_9))
                .isInstanceOf(com.ieltspath.content.domain.exception.PlacementPoolLockedException.class);
        assertThat(pkg.getPlacementPool()).isEqualTo(com.ieltspath.content.domain.vo.PlacementPool.BAND_3_5);
    }

    @Test
    @DisplayName("Only a practice test has a source, and only a placement test has a pool")
    void sourceAndPoolBelongToTheirType() {
        ContentPackage quiz = ContentPackage.create("QZ_01", "Quiz", PackageType.QUIZ, null);
        assertThatThrownBy(() -> quiz.changeSource("CAMBRIDGE")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> quiz.changePlacementPool(com.ieltspath.content.domain.vo.PlacementPool.BAND_0_3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Access level and title change on any package")
    void accessLevelAndTitleChange() {
        ContentPackage pkg = ContentPackage.create("PT_01", "Reading 1", PackageType.PRACTICE_TEST, null);
        pkg.changeRequiredFeatureKey("PREMIUM_CONTENT");
        pkg.rename("  Reading 1 (mới)  ");
        assertThat(pkg.getRequiredFeatureKey()).isEqualTo("PREMIUM_CONTENT");
        assertThat(pkg.getTitle()).isEqualTo("Reading 1 (mới)");
        assertThatThrownBy(() -> pkg.rename(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Archive and restore go back to the right status")
    void archiveAndRestore() {
        ContentPackage unpublished = ContentPackage.create("PT_02", "Reading 2", PackageType.PRACTICE_TEST, null);
        unpublished.archive();
        assertThat(unpublished.getStatus()).isEqualTo(PublicationStatus.ARCHIVED);
        unpublished.restore();
        assertThat(unpublished.getStatus()).isEqualTo(PublicationStatus.DRAFT);

        ContentPackage published = ContentPackage.create("PT_03", "Reading 3", PackageType.PRACTICE_TEST, null);
        ContentPackageVersion v1 = ContentPackageVersion.create(published.getId(), 1, "{}");
        published.addVersion(v1);
        published.publishVersion(v1.getId());
        published.archive();
        published.restore();
        assertThat(published.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);

        ContentPackage quiz = ContentPackage.create("QZ_02", "Quiz", PackageType.QUIZ, null);
        assertThatThrownBy(quiz::archive)
                .isInstanceOf(com.ieltspath.content.domain.exception.PackageArchiveNotSupportedException.class);
    }

    @Test
    @DisplayName("Should throw exception when publishing non-existent version")
    void shouldThrowExceptionWhenPublishingNonExistentVersion() {
        ContentPackage pkg = ContentPackage.create("PKG_01", "Cambridge 18 Test 1", PackageType.MOCK_TEST, null);
        UUID randomVersionId = UUID.randomUUID();

        assertThatThrownBy(() -> pkg.publishVersion(randomVersionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phiên bản không thuộc gói");
    }

    @Test
    @DisplayName("Only a practice set can belong to a lesson")
    void onlyAPracticeSetCanBelongToALesson() {
        UUID lessonId = UUID.randomUUID();
        assertThatThrownBy(() -> ContentPackage.create("MOCK", "Mock", PackageType.MOCK_TEST, null, lessonId))
                .isInstanceOf(InvalidPackageLessonException.class)
                .hasMessage("Chỉ gói luyện tập (PRACTICE_SET) mới gắn vào bài học được");
        assertThat(ContentPackage.create("PS", "Practice", PackageType.PRACTICE_SET, null, lessonId).getLessonId())
                .isEqualTo(lessonId);
    }
}
