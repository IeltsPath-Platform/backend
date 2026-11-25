package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.NewerQuestionVersionFinder;
import com.ieltspath.content.application.result.AuthorSectionQuestionResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.entity.SectionQuestion;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseNewerSectionQuestionVersionUseCaseTest {

    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final NewerQuestionVersionFinder newerVersions = mock(NewerQuestionVersionFinder.class);
    private final UseNewerSectionQuestionVersionUseCase useCase = new UseNewerSectionQuestionVersionUseCase(packages, newerVersions);

    private ContentPackage pkg;
    private ContentPackageVersion version;
    private ContentSection section;
    private SectionQuestion row;
    private final UUID oldVersion = UUID.randomUUID();

    private void givenDraftWritingSection() {
        pkg = ContentPackage.create("PT-W", "Writing", PackageType.PRACTICE_SET, null);
        version = ContentPackageVersion.create(pkg.getId(), 1, "{}");
        section = ContentSection.create(version.getId(), "Task 1", Skill.WRITING, 1, null, null);
        version.addSection(section);
        row = SectionQuestion.create(section.getId(), oldVersion, 1, new BigDecimal("2"));
        section.addQuestion(row);
        pkg.addVersion(version);
        when(packages.findByIdForUpdate(pkg.getId())).thenReturn(Optional.of(pkg));
    }

    @Test
    void theRowMovesToTheNewerVersionAndKeepsItsPlaceAndScore() {
        givenDraftWritingSection();
        UUID newer = UUID.randomUUID();
        when(newerVersions.newerPublishedVersion(oldVersion)).thenReturn(Optional.of(newer));

        AuthorSectionQuestionResult result = useCase.execute(pkg.getId(), version.getId(), section.getId(), row.getId());

        assertThat(result.questionVersionId()).isEqualTo(newer);
        assertThat(result.maxScore()).isEqualByComparingTo("2");
        assertThat(row.getQuestionVersionId()).isEqualTo(newer);
        assertThat(section.getQuestions()).hasSize(1);
        verify(packages).save(pkg);
    }

    @Test
    void aQuestionWithNoNewerPublishedVersionIsRefused() {
        givenDraftWritingSection();
        when(newerVersions.newerPublishedVersion(oldVersion)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(pkg.getId(), version.getId(), section.getId(), row.getId()))
                .isInstanceOf(InvalidContentStateException.class);
        assertThat(row.getQuestionVersionId()).isEqualTo(oldVersion);
        verify(packages, never()).save(pkg);
    }
}
