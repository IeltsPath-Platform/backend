package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchedulePackagePublicationUseCaseTest {
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final PublishContentPackageUseCase publish = mock(PublishContentPackageUseCase.class);
    private final SchedulePackagePublicationUseCase useCase = new SchedulePackagePublicationUseCase(packages, publish);

    @Test
    void scheduleMustBeInTheFuture() {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        assertThatThrownBy(() -> useCase.execute(packageId, versionId, Instant.now().minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("publishAt must be in the future");
        verify(packages, never()).findById(packageId);
    }

    @Test
    void draftIsValidatedAndSavedWithItsSchedule() {
        ContentPackage pkg = packageWithVersion(PublicationStatus.DRAFT);
        ContentPackageVersion version = pkg.getVersions().getFirst();
        when(packages.findById(pkg.getId())).thenReturn(Optional.of(pkg));
        when(packages.save(pkg)).thenReturn(pkg);
        Instant publishAt = Instant.now().plusSeconds(300);

        var result = useCase.execute(pkg.getId(), version.getId(), publishAt);

        assertThat(result.publishAt()).isEqualTo(publishAt);
        assertThat(result.lastPublishError()).isNull();
        verify(publish).validateForPublish(pkg.getId(), version.getId());
        verify(packages).save(pkg);
    }

    @Test
    void publishedVersionCannotBeScheduled() {
        ContentPackage pkg = packageWithVersion(PublicationStatus.PUBLISHED);
        ContentPackageVersion version = pkg.getVersions().getFirst();
        when(packages.findById(pkg.getId())).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> useCase.execute(pkg.getId(), version.getId(), Instant.now().plusSeconds(60)))
                .isInstanceOf(InvalidContentStateException.class);
        verify(publish, never()).validateForPublish(pkg.getId(), version.getId());
        verify(packages, never()).save(pkg);
    }

    private static ContentPackage packageWithVersion(PublicationStatus status) {
        UUID packageId = UUID.randomUUID();
        ContentPackageVersion version = new ContentPackageVersion(UUID.randomUUID(), packageId, 1,
                status, "{}", 1, null, null, null, null, Instant.now(), Instant.now(), new ArrayList<>());
        return new ContentPackage(packageId, "SCHEDULE_TEST", "Schedule test", PackageType.PRACTICE_SET,
                null, PublicationStatus.DRAFT, null, Instant.now(), Instant.now(), List.of(version), null);
    }
}
