package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.PublishContentPackageCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.QuestionAlreadyUsedException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionUsageConflict;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledPackagePublicationProcessorTest {

    @Test
    void aTimedPublishRunsThePublishUseCaseWithNoAuthor() {
        PublishContentPackageUseCase publish = mock(PublishContentPackageUseCase.class);
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        new ScheduledPackagePublicationProcessor(publish).publish(packageId, versionId);

        ArgumentCaptor<PublishContentPackageCommand> command = ArgumentCaptor.forClass(PublishContentPackageCommand.class);
        verify(publish).execute(command.capture());
        assertThat(command.getValue().packageId()).isEqualTo(packageId);
        assertThat(command.getValue().versionId()).isEqualTo(versionId);
        assertThat(command.getValue().publishedBy()).isNull();
    }

    @Test
    void aRuleBrokenAfterSchedulingStopsTheTimedPublish() {
        ContentPackageRepository packages = mock(ContentPackageRepository.class);
        LearningContentReader reader = mock(LearningContentReader.class);
        ContentPackage pkg = ContentPackage.create("MOCK-TIMED", "Mock", PackageType.MOCK_TEST, null);
        ContentPackageVersion version = ContentPackageVersion.create(pkg.getId(), 1, "{}");
        pkg.addVersion(version);
        version.schedulePublication(Instant.now().plusSeconds(60));
        when(packages.findById(pkg.getId())).thenReturn(Optional.of(pkg));
        when(reader.questionsUsedElsewhere(version.getId())).thenReturn(List.of(
                new QuestionUsageConflict(UUID.randomUUID(), QuestionUsageConflict.OwnerType.PACKAGE, "OTHER")));

        assertThatThrownBy(() -> new ScheduledPackagePublicationProcessor(new PublishContentPackageUseCase(packages, reader))
                .publish(pkg.getId(), version.getId())).isInstanceOf(QuestionAlreadyUsedException.class);
        assertThat(version.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        verify(packages, never()).save(any());
    }
}
