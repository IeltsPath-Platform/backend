package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.exception.PackageArchiveNotSupportedException;
import com.ieltspath.content.domain.exception.PlacementPoolLastPublishedException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PlacementPool;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangePackageVisibilityUseCaseTest {
    private ContentPackageRepository packages;
    private ChangePackageVisibilityUseCase useCase;

    @BeforeEach
    void setUp() {
        packages = mock(ContentPackageRepository.class);
        when(packages.save(any())).thenAnswer(call -> call.getArgument(0));
        useCase = new ChangePackageVisibilityUseCase(packages);
    }

    private ContentPackage published(PackageType type, PlacementPool pool) {
        ContentPackage pkg = ContentPackage.create("P_" + UUID.randomUUID().toString().substring(0, 6), "Đề", type, null,
                null, null, pool);
        ContentPackageVersion version = ContentPackageVersion.create(pkg.getId(), 1, "{}");
        pkg.addVersion(version);
        pkg.publishVersion(version.getId());
        when(packages.findByIdForUpdate(pkg.getId())).thenReturn(Optional.of(pkg));
        return pkg;
    }

    @Test
    void archivesAPracticeTestAndRestoresItToPublished() {
        ContentPackage pkg = published(PackageType.PRACTICE_TEST, null);

        assertThat(useCase.archive(pkg.getId()).status()).isEqualTo(PublicationStatus.ARCHIVED);
        assertThat(useCase.restore(pkg.getId()).status()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void lastPublishedPlacementTestOfAPoolStaysUp() {
        ContentPackage pkg = published(PackageType.PLACEMENT_TEST, PlacementPool.BAND_0_3);
        when(packages.countPublishedPlacementInPool(PlacementPool.BAND_0_3, pkg.getId())).thenReturn(0L);

        assertThatThrownBy(() -> useCase.archive(pkg.getId())).isInstanceOf(PlacementPoolLastPublishedException.class);
        assertThat(pkg.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        verify(packages, never()).save(any());
    }

    @Test
    void aPlacementTestIsArchivedWhenAnotherOneIsPublishedInThePool() {
        ContentPackage pkg = published(PackageType.PLACEMENT_TEST, PlacementPool.BAND_3_5);
        when(packages.countPublishedPlacementInPool(PlacementPool.BAND_3_5, pkg.getId())).thenReturn(1L);

        assertThat(useCase.archive(pkg.getId()).status()).isEqualTo(PublicationStatus.ARCHIVED);
    }

    @Test
    void anUnpublishedPlacementTestCanBeArchivedEvenWhenItIsTheOnlyOne() {
        ContentPackage draft = ContentPackage.create("PL_DRAFT", "Nháp", PackageType.PLACEMENT_TEST, null, null, null,
                PlacementPool.BAND_5_9);
        when(packages.findByIdForUpdate(draft.getId())).thenReturn(Optional.of(draft));

        assertThat(useCase.archive(draft.getId()).status()).isEqualTo(PublicationStatus.ARCHIVED);
        assertThat(useCase.restore(draft.getId()).status()).isEqualTo(PublicationStatus.DRAFT);
    }

    @Test
    void otherTypesAreRefused() {
        ContentPackage quiz = published(PackageType.QUIZ, null);

        assertThatThrownBy(() -> useCase.archive(quiz.getId())).isInstanceOf(PackageArchiveNotSupportedException.class);
    }

    @Test
    void unknownPackageIsNotFound() {
        UUID id = UUID.randomUUID();
        when(packages.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.archive(id)).isInstanceOf(ContentPackageNotFoundException.class);
    }
}
