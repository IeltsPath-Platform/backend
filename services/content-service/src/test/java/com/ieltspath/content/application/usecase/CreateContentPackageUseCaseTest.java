package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A practice test without a source is an internal course checkpoint; a source, when given, must be supported. */
class CreateContentPackageUseCaseTest {
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final LearningContentReader reader = mock(LearningContentReader.class);
    private final CreateContentPackageUseCase useCase = new CreateContentPackageUseCase(packages, reader);

    private void savingEchoesThePackage() {
        when(packages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void aPracticeTestCanBeCreatedWithoutASourceAsAnInternalCheckpoint() {
        savingEchoesThePackage();

        var created = useCase.execute(new CreateContentPackageCommand("CHECKPOINT-1", "Checkpoint",
                PackageType.PRACTICE_TEST, null, null, null));

        assertThat(created.sourceCode()).isNull();
        assertThat(created.packageType()).isEqualTo(PackageType.PRACTICE_TEST);
    }

    @Test
    void aPracticeTestWithASupportedSourceIsCreatedForTheLibrary() {
        savingEchoesThePackage();

        var created = useCase.execute(new CreateContentPackageCommand("LIBRARY-1", "Library test",
                PackageType.PRACTICE_TEST, null, null, "IELTS_SPACE_PRO"));

        assertThat(created.sourceCode()).isEqualTo("IELTS_SPACE_PRO");
    }

    @Test
    void anUnknownSourceIsRefused() {
        assertThatThrownBy(() -> useCase.execute(new CreateContentPackageCommand("BAD-SOURCE", "Bad",
                PackageType.PRACTICE_TEST, null, null, "NOT_A_SOURCE")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sourceCode");
        verify(packages, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class, names = {"PRACTICE_SET", "QUIZ", "MOCK_TEST", "PLACEMENT_TEST"})
    void aSourceOnAnyOtherPackageTypeIsRefused(PackageType type) {
        assertThatThrownBy(() -> useCase.execute(new CreateContentPackageCommand("OTHER-" + type, "Other", type, null,
                null, "IELTS_SPACE_PRO")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("only valid for PRACTICE_TEST");
        verify(packages, never()).save(any());
    }
}
