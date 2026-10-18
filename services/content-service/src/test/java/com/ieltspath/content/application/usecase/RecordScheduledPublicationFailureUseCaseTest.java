package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordScheduledPublicationFailureUseCaseTest {
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final RecordScheduledPublicationFailureUseCase useCase = new RecordScheduledPublicationFailureUseCase(packages);

    private ContentPackageVersion dueVersion() {
        ContentPackage pkg = ContentPackage.create("SCHEDULED-FAILURE", "Scheduled", PackageType.PRACTICE_SET, null);
        ContentPackageVersion version = ContentPackageVersion.create(pkg.getId(), 1, "{}");
        pkg.addVersion(version);
        version.schedulePublication(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(packages.findById(pkg.getId())).thenReturn(Optional.of(pkg));
        return version;
    }

    @ParameterizedTest
    @ValueSource(strings = {"QUESTION_ALREADY_USED", "QUESTION_PURPOSE_MISMATCH", "INVALID_PACKAGE_LESSON",
            "INVALID_CONTENT_STATE"})
    void aBrokenPublishingRuleDropsTheDueTimeAndKeepsTheReason(String code) {
        ContentPackageVersion version = dueVersion();

        useCase.execute(version.getPackageId(), version.getId(), code);

        assertThat(version.getPublishAt()).as("not retried until an author reschedules").isNull();
        assertThat(version.getLastPublishError()).isEqualTo(code);
    }

    @Test
    void anUnknownFailureKeepsTheDueTimeSoTheJobTriesAgain() {
        ContentPackageVersion version = dueVersion();

        useCase.execute(version.getPackageId(), version.getId(), RecordScheduledPublicationFailureUseCase.UNKNOWN_FAILURE);

        assertThat(version.getPublishAt()).isNotNull();
        assertThat(version.getLastPublishError()).isEqualTo("PUBLISH_FAILED");
    }

    @Test
    void reschedulingAfterARejectionClearsTheError() {
        ContentPackageVersion version = dueVersion();
        useCase.execute(version.getPackageId(), version.getId(), "QUESTION_ALREADY_USED");

        version.schedulePublication(Instant.now().plus(1, ChronoUnit.HOURS));

        assertThat(version.getPublishAt()).isNotNull();
        assertThat(version.getLastPublishError()).isNull();
    }
}
