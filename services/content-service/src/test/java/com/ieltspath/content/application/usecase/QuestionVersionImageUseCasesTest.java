package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.QuestionImageWriter;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.exception.QuestionNotFoundException;
import com.ieltspath.content.domain.exception.QuestionVersionNotFoundException;
import com.ieltspath.content.domain.exception.QuestionVersionStateException;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QuestionVersionImageUseCasesTest {

    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final QuestionImageWriter writer = mock(QuestionImageWriter.class);
    private final QuestionVersionImageUseCases useCase = new QuestionVersionImageUseCases(questions, writer);

    private Question question(Skill skill, boolean publish) {
        return question(skill, publish, false);
    }

    private Question question(Skill skill, boolean publish, boolean archive) {
        Question question = Question.create(QuestionType.ESSAY, skill, null);
        QuestionVersion version = QuestionVersion.create(question.getId(), 1, "Describe the chart.", List.of(),
                "{\"type\":\"ESSAY\"}", null, QuestionDifficulty.MEDIUM);
        question.addVersion(version);
        if (publish) question.publishVersion(version.getId());
        if (archive) question.archiveVersion(version.getId());
        when(questions.findByIdForUpdate(question.getId())).thenReturn(Optional.of(question));
        return question;
    }

    @Test
    void theImageIsSetOnADraftWritingVersionWithATrimmedDescription() {
        Question question = question(Skill.WRITING, false);
        UUID version = question.getVersions().get(0).getId();

        useCase.set(question.getId(), version, "content-image/user/chart.png", "  Bar chart of visitors  ");

        verify(writer).replace(version, "content-image/user/chart.png", "Bar chart of visitors");
    }

    @Test
    void aBlankDescriptionIsStoredAsNone() {
        Question question = question(Skill.WRITING, false);
        UUID version = question.getVersions().get(0).getId();

        useCase.set(question.getId(), version, "content-image/user/chart.png", "   ");

        verify(writer).replace(version, "content-image/user/chart.png", null);
    }

    @Test
    void theImageIsClearedFromADraftVersion() {
        Question question = question(Skill.WRITING, false);
        UUID version = question.getVersions().get(0).getId();

        useCase.clear(question.getId(), version);

        verify(writer).clear(version);
    }

    @Test
    void aPublishedVersionTakesAnImageToo() {
        Question question = question(Skill.WRITING, true);
        UUID version = question.getVersions().get(0).getId();

        useCase.set(question.getId(), version, "content-image/user/chart.png", null);
        useCase.clear(question.getId(), version);

        verify(writer).replace(version, "content-image/user/chart.png", null);
        verify(writer).clear(version);
    }

    @Test
    void anArchivedVersionIsNotChanged() {
        Question question = question(Skill.WRITING, true, true);
        UUID version = question.getVersions().get(0).getId();

        assertThatThrownBy(() -> useCase.set(question.getId(), version, "content-image/user/chart.png", null))
                .isInstanceOf(QuestionVersionStateException.class);
        assertThatThrownBy(() -> useCase.clear(question.getId(), version)).isInstanceOf(QuestionVersionStateException.class);
        verifyNoInteractions(writer);
    }

    @Test
    void onlyWritingQuestionsTakeAnImage() {
        Question question = question(Skill.READING, false);
        UUID version = question.getVersions().get(0).getId();

        assertThatThrownBy(() -> useCase.set(question.getId(), version, "content-image/user/chart.png", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Writing");
        verifyNoInteractions(writer);
    }

    @Test
    void anUnsafeKeyOrAMissingKeyOrAHugeDescriptionIsRefused() {
        Question question = question(Skill.WRITING, false);
        UUID version = question.getVersions().get(0).getId();

        assertThatThrownBy(() -> useCase.set(question.getId(), version, "../secret.png", null)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> useCase.set(question.getId(), version, " ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.set(question.getId(), version, "content-image/user/chart.png", "x".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(writer, org.mockito.Mockito.never()).replace(any(), anyString(), any());
    }

    @Test
    void anUnknownQuestionOrVersionIsNotFound() {
        UUID unknown = UUID.randomUUID();
        when(questions.findByIdForUpdate(unknown)).thenReturn(Optional.empty());
        Question question = question(Skill.WRITING, false);

        assertThatThrownBy(() -> useCase.clear(unknown, UUID.randomUUID())).isInstanceOf(QuestionNotFoundException.class);
        assertThatThrownBy(() -> useCase.clear(question.getId(), UUID.randomUUID())).isInstanceOf(QuestionVersionNotFoundException.class);
    }
}
