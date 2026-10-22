package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.exception.QuestionVersionNotPublishableException;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublishQuestionVersionUseCaseTest {
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final PublishQuestionVersionUseCase useCase = new PublishQuestionVersionUseCase(questions, new ObjectMapper());

    @Test
    void publishesValidDraftVersionAndSelectsItOnQuestion() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        QuestionVersion version = QuestionVersion.create(question.getId(), 1, "Stem",
                List.of(new QuestionOptionPayload("A", "One", 1)),
                "{\"type\":\"CHOICE\",\"correct\":\"A\"}", null, QuestionDifficulty.MEDIUM);
        question.addVersion(version);
        when(questions.findByIdForUpdate(question.getId())).thenReturn(Optional.of(question));
        when(questions.save(question)).thenReturn(question);

        var result = useCase.execute(question.getId(), version.getId());

        assertThat(result.currentPublishedVersionId()).isEqualTo(version.getId());
        assertThat(question.getVersions().get(0).getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void rejectsInvalidDraftAnswerSpecification() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        QuestionVersion version = QuestionVersion.create(question.getId(), 1, "Stem", List.of(),
                "{\"type\":\"CHOICE\",\"correct\":\"Z\"}", null, QuestionDifficulty.MEDIUM);
        question.addVersion(version);
        when(questions.findByIdForUpdate(question.getId())).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> useCase.execute(question.getId(), version.getId()))
                .isInstanceOf(QuestionVersionNotPublishableException.class);
        assertThat(question.getCurrentPublishedVersionId()).isNull();
    }
}
