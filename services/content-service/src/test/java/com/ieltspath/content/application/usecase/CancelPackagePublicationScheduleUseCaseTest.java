package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelPackagePublicationScheduleUseCaseTest {
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final CancelPackagePublicationScheduleUseCase useCase = new CancelPackagePublicationScheduleUseCase(packages);
    private final ContentPackage pkg = ContentPackage.create("PS-SCHEDULE", "Practice", PackageType.PRACTICE_SET, null);
    private final ContentPackageVersion version = ContentPackageVersion.create(pkg.getId(), 1, "{}");

    CancelPackagePublicationScheduleUseCaseTest() {
        pkg.addVersion(version);
        when(packages.findById(pkg.getId())).thenReturn(Optional.of(pkg));
    }

    @Test
    void aDraftScheduleIsCancelled() {
        version.schedulePublication(Instant.now().plusSeconds(86_400));
        useCase.execute(pkg.getId(), version.getId());
        assertThat(version.getPublishAt()).isNull();
        verify(packages).save(pkg);
    }

    @Test
    void aPublishedVersionHasNoScheduleToCancel() {
        version.publish(UUID.randomUUID());
        assertThatThrownBy(() -> useCase.execute(pkg.getId(), version.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only DRAFT versions can have a publication schedule");
        verify(packages, never()).save(any());
    }
}
