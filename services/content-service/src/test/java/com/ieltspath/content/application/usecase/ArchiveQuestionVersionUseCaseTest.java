package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.QuestionVersionUsageLookup;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.exception.QuestionVersionInUseException;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArchiveQuestionVersionUseCaseTest {
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final QuestionVersionUsageLookup usage = mock(QuestionVersionUsageLookup.class);
    private final ArchiveQuestionVersionUseCase useCase = new ArchiveQuestionVersionUseCase(questions, usage);

    @Test
    void refusesToArchiveVersionReferencedByPublishedContent() {
        Question question = publishedQuestion();
        var version = question.getVersions().getFirst();
        when(questions.findByIdForUpdate(question.getId())).thenReturn(Optional.of(question));
        when(usage.isUsedByPublishedContent(version.getId())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(question.getId(), version.getId()))
                .isInstanceOf(QuestionVersionInUseException.class);
        assertThat(version.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void archivesAnUnusedPublishedVersionAndClearsTheCurrentPointer() {
        Question question = publishedQuestion();
        var version = question.getVersions().getFirst();
        when(questions.findByIdForUpdate(question.getId())).thenReturn(Optional.of(question));
        when(usage.isUsedByPublishedContent(version.getId())).thenReturn(false);
        when(questions.save(question)).thenReturn(question);

        useCase.execute(question.getId(), version.getId());

        assertThat(version.getStatus()).isEqualTo(PublicationStatus.ARCHIVED);
        assertThat(question.getCurrentPublishedVersionId()).isNull();
        assertThat(question.getStatus()).isEqualTo(PublicationStatus.DRAFT);
    }

    private static Question publishedQuestion() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        QuestionVersion version = QuestionVersion.create(question.getId(), 1, "Stem",
                List.of(new QuestionOptionPayload("A", "One", 1)),
                "{\"type\":\"CHOICE\",\"correct\":\"A\"}", null, QuestionDifficulty.MEDIUM);
        question.addVersion(version);
        question.publishVersion(version.getId());
        return question;
    }
}
