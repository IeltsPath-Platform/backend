package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.QuestionAlreadyInVersionException;
import com.ieltspath.content.domain.exception.QuestionVersionNotFoundException;
import com.ieltspath.content.domain.exception.VersionNotDraftException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddSectionQuestionUseCaseTest {
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final AddSectionQuestionUseCase useCase = new AddSectionQuestionUseCase(packages);
    private final ContentPackage pkg = ContentPackage.create("PS-ADD", "Practice", PackageType.PRACTICE_SET, null);
    private final ContentPackageVersion version = ContentPackageVersion.create(pkg.getId(), 1, "{}");
    private final ContentSection section = ContentSection.create(version.getId(), "Passage 1", null, 1, null, null);
    private final ContentSection other = ContentSection.create(version.getId(), "Passage 2", null, 2, null, null);
    private final UUID questionVersion = UUID.randomUUID();

    AddSectionQuestionUseCaseTest() {
        version.addSection(section);
        version.addSection(other);
        pkg.addVersion(version);
        when(packages.findByIdForUpdate(pkg.getId())).thenReturn(Optional.of(pkg));
        when(packages.questionVersionExists(questionVersion)).thenReturn(true);
    }

    private void add(UUID sectionId, UUID questionVersionId, BigDecimal score) {
        useCase.execute(pkg.getId(), version.getId(), sectionId, questionVersionId, score);
    }

    @Test
    void aPublishedVersionCannotTakeANewQuestion() {
        version.publish(UUID.randomUUID());
        assertThatThrownBy(() -> add(section.getId(), questionVersion, BigDecimal.ONE)).isInstanceOf(VersionNotDraftException.class);
        assertThat(section.getQuestions()).isEmpty();
        verify(packages, never()).save(any());
    }

    @Test
    void theScoreMustBePositiveAndDefaultsToOne() {
        assertThatThrownBy(() -> add(section.getId(), questionVersion, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("maxScore must be positive");
        assertThatThrownBy(() -> add(section.getId(), questionVersion, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("maxScore must be positive");
        var result = useCase.execute(pkg.getId(), version.getId(), section.getId(), questionVersion, null);
        assertThat(result.maxScore()).isEqualByComparingTo("1");
        verify(packages).save(pkg);
    }

    @Test
    void aMissingOrUnknownQuestionVersionIsRefused() {
        assertThatThrownBy(() -> add(section.getId(), null, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("questionVersionId is required");
        UUID unknown = UUID.randomUUID();
        assertThatThrownBy(() -> add(section.getId(), unknown, BigDecimal.ONE)).isInstanceOf(QuestionVersionNotFoundException.class);
        verify(packages, never()).save(any());
    }

    @Test
    void theSameQuestionCannotAppearTwiceInOneVersion() {
        add(section.getId(), questionVersion, BigDecimal.ONE);
        assertThatThrownBy(() -> add(other.getId(), questionVersion, BigDecimal.ONE))
                .isInstanceOf(QuestionAlreadyInVersionException.class);
        assertThat(other.getQuestions()).isEmpty();
    }
}
