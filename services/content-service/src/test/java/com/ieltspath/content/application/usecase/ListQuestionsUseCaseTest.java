package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ListQuestionsUseCaseTest {

    @Mock QuestionRepository questions;

    @Test
    void passesFiltersAndPagingToTheRepositoryAndReportsTotalPages() {
        when(questions.findPage(Skill.READING, QuestionPurpose.LEARNING, PublicationStatus.PUBLISHED, 1, 10))
                .thenReturn(new QuestionRepository.Page(23, List.of()));

        ListQuestionsUseCase.Page page = new ListQuestionsUseCase(questions)
                .execute(Skill.READING, QuestionPurpose.LEARNING, PublicationStatus.PUBLISHED, 1, 10);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(23);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void emptyCatalogHasNoPages() {
        when(questions.findPage(null, null, null, 0, 25)).thenReturn(new QuestionRepository.Page(0, List.of()));

        assertThat(new ListQuestionsUseCase(questions).execute(null, null, null, 0, 25).totalPages()).isZero();
    }

    @Test
    void rejectsPagingOutsideTheAllowedRangeWithoutQueryingTheDatabase() {
        ListQuestionsUseCase useCase = new ListQuestionsUseCase(questions);

        assertThatThrownBy(() -> useCase.execute(null, null, null, -1, 25)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(null, null, null, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(null, null, null, 0, ListQuestionsUseCase.MAX_PAGE_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
        verify(questions, never()).findPage(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void blankImportReferenceFindsNothing() {
        ListQuestionsUseCase.Page page = new ListQuestionsUseCase(questions).findByImportRef("   ");

        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isZero();
        verify(questions, never()).findByImportRef(any());
    }

    @Test
    void unknownImportReferenceFindsNothing() {
        when(questions.findByImportRef("missing")).thenReturn(Optional.empty());

        assertThat(new ListQuestionsUseCase(questions).findByImportRef("missing").items()).isEmpty();
    }
}
