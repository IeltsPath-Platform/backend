package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.entity.SectionQuestion;
import com.ieltspath.content.domain.exception.VersionNotDraftException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

class RemoveAndReorderSectionQuestionsUseCaseTest {
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final RemoveAndReorderSectionQuestionsUseCase useCase = new RemoveAndReorderSectionQuestionsUseCase(packages);

    @Test
    void aPublishedVersionCannotLoseOrReorderQuestions() {
        ContentPackage pkg = ContentPackage.create("PS-LIVE", "Practice", PackageType.PRACTICE_SET, null);
        ContentPackageVersion version = ContentPackageVersion.create(pkg.getId(), 1, "{}");
        ContentSection section = ContentSection.create(version.getId(), "Passage", null, 1, null, null);
        version.addSection(section);
        SectionQuestion first = SectionQuestion.create(section.getId(), UUID.randomUUID(), 1, BigDecimal.ONE);
        SectionQuestion second = SectionQuestion.create(section.getId(), UUID.randomUUID(), 2, BigDecimal.ONE);
        section.addQuestion(first);
        section.addQuestion(second);
        pkg.addVersion(version);
        version.publish(UUID.randomUUID());
        when(packages.findByIdForUpdate(pkg.getId())).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> useCase.remove(pkg.getId(), version.getId(), section.getId(), first.getId()))
                .isInstanceOf(VersionNotDraftException.class);
        assertThatThrownBy(() -> useCase.reorder(pkg.getId(), version.getId(), section.getId(),
                List.of(second.getId(), first.getId()))).isInstanceOf(VersionNotDraftException.class);

        assertThat(section.getQuestions()).hasSize(2);
        assertThat(first.getSortOrder()).isEqualTo(1);
        verify(packages, never()).deferQuestionOrderCheck();
        verify(packages, never()).save(any());
    }
}
