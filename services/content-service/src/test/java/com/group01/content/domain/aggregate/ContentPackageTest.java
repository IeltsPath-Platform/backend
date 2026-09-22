package com.group01.content.domain.aggregate;

import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.PublicationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentPackageTest {

    @Test
    @DisplayName("Should create content package in DRAFT status")
    void shouldCreatePackageInDraftStatus() {
        ContentPackage pkg = ContentPackage.create("PKG_01", "Cambridge 18 Test 1", PackageType.MOCK_TEST, AccessLevel.FREE);

        assertThat(pkg.getId()).isNotNull();
        assertThat(pkg.getCode()).isEqualTo("PKG_01");
        assertThat(pkg.getTitle()).isEqualTo("Cambridge 18 Test 1");
        assertThat(pkg.getPackageType()).isEqualTo(PackageType.MOCK_TEST);
        assertThat(pkg.getAccessLevel()).isEqualTo(AccessLevel.FREE);
        assertThat(pkg.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(pkg.getCurrentPublishedVersionId()).isNull();
    }

    @Test
    @DisplayName("Should add version and publish version correctly")
    void shouldAddAndPublishVersion() {
        ContentPackage pkg = ContentPackage.create("PKG_01", "Cambridge 18 Test 1", PackageType.MOCK_TEST, AccessLevel.FREE);
        ContentPackageVersion v1 = ContentPackageVersion.create(pkg.getId(), 1, "{}");

        pkg.addVersion(v1);
        assertThat(pkg.getVersions()).hasSize(1);

        pkg.publishVersion(v1.getId());
        assertThat(pkg.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(pkg.getCurrentPublishedVersionId()).isEqualTo(v1.getId());
    }

    @Test
    @DisplayName("Should throw exception when publishing non-existent version")
    void shouldThrowExceptionWhenPublishingNonExistentVersion() {
        ContentPackage pkg = ContentPackage.create("PKG_01", "Cambridge 18 Test 1", PackageType.MOCK_TEST, AccessLevel.FREE);
        UUID randomVersionId = UUID.randomUUID();

        assertThatThrownBy(() -> pkg.publishVersion(randomVersionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Version does not belong to package");
    }
}

