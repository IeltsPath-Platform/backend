package com.ieltspath.content.application.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import java.util.UUID;
import com.ieltspath.content.domain.exception.QuestionVersionNotFoundException;
import com.ieltspath.content.domain.exception.QuestionNotFoundException;
import com.ieltspath.content.domain.exception.QuestionDifficultyRequiredForPublicationException;
import com.ieltspath.content.domain.exception.QuestionVersionStateException;
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

    private Question questionWith(QuestionType type, String spec, List<QuestionOptionPayload> options,
                                  QuestionDifficulty difficulty) {
        Question question = Question.create(type, type == QuestionType.ESSAY ? Skill.WRITING : Skill.READING, null);
        question.addVersion(QuestionVersion.create(question.getId(), 1, "Stem", options, spec, null, difficulty));
        when(questions.findByIdForUpdate(question.getId())).thenReturn(Optional.of(question));
        return question;
    }

    private static final List<QuestionOptionPayload> AB = List.of(
            new QuestionOptionPayload("A", "One", 1), new QuestionOptionPayload("B", "Two", 2));

    private boolean publishes(Question question) {
        try {
            useCase.execute(question.getId(), question.getVersions().getFirst().getId());
            return true;
        } catch (QuestionVersionNotPublishableException refused) {
            return false;
        }
    }

    @Test
    void aChoiceWithTwoOptionsAndAValidKeyIsPublished() {
        Question question = questionWith(QuestionType.MULTIPLE_CHOICE, "{\"type\":\"CHOICE\",\"correct\":\"B\"}", AB,
                QuestionDifficulty.MEDIUM);
        when(questions.save(question)).thenReturn(question);
        assertThat(publishes(question)).isTrue();
        assertThat(question.getCurrentPublishedVersionId()).isEqualTo(question.getVersions().getFirst().getId());
    }

    @Test
    void onlyADraftVersionCanBePublished() {
        Question question = questionWith(QuestionType.MULTIPLE_CHOICE, "{\"type\":\"CHOICE\",\"correct\":\"A\"}", AB,
                QuestionDifficulty.MEDIUM);
        UUID versionId = question.getVersions().getFirst().getId();
        question.publishVersion(versionId);
        assertThatThrownBy(() -> useCase.execute(question.getId(), versionId))
                .isInstanceOf(QuestionVersionStateException.class)
                .hasMessage("Only draft question versions can be published");
        question.archiveVersion(versionId);
        assertThatThrownBy(() -> useCase.execute(question.getId(), versionId))
                .isInstanceOf(QuestionVersionStateException.class);
        verify(questions, never()).save(any());
    }

    @Test
    void aVersionWithoutDifficultyCannotBePublished() {
        Question question = questionWith(QuestionType.MULTIPLE_CHOICE, "{\"type\":\"CHOICE\",\"correct\":\"A\"}", AB, null);
        assertThatThrownBy(() -> useCase.execute(question.getId(), question.getVersions().getFirst().getId()))
                .isInstanceOf(QuestionDifficultyRequiredForPublicationException.class);
        verify(questions, never()).save(any());
    }

    @Test
    void unknownQuestionsAndVersionsAreNotFound() {
        UUID missing = UUID.randomUUID();
        when(questions.findByIdForUpdate(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(missing, UUID.randomUUID())).isInstanceOf(QuestionNotFoundException.class);
        Question question = questionWith(QuestionType.MULTIPLE_CHOICE, "{\"type\":\"CHOICE\",\"correct\":\"A\"}", AB,
                QuestionDifficulty.MEDIUM);
        assertThatThrownBy(() -> useCase.execute(question.getId(), UUID.randomUUID()))
                .isInstanceOf(QuestionVersionNotFoundException.class);
    }

    @Test
    void everyMalformedChoiceSpecIsRefused() {
        for (String spec : List.of("not json", "[]", "{\"type\":\"CHOICE\",\"correct\":\" \"}",
                "{\"type\":\"CHOICE\",\"correct\":1}", "{\"type\":\"MATCH\",\"correct\":\"A\"}")) {
            assertThat(publishes(questionWith(QuestionType.MULTIPLE_CHOICE, spec, AB, QuestionDifficulty.MEDIUM)))
                    .as(spec).isFalse();
        }
        verify(questions, never()).save(any());
    }

    @Test
    void everyMalformedFillSpecIsRefused() {
        for (String spec : List.of("{\"type\":\"FILL\",\"accepted\":[]}", "{\"type\":\"FILL\",\"accepted\":[\"x\",\" \"]}",
                "{\"type\":\"FILL\",\"accepted\":[\"x\",2]}")) {
            assertThat(publishes(questionWith(QuestionType.FILL_IN_BLANK, spec, List.of(), QuestionDifficulty.MEDIUM)))
                    .as(spec).isFalse();
        }
    }

    @Test
    void anEssayPassBandRunsFromFourToNineInHalfBands() {
        for (String band : List.of("4.0", "9.0")) {
            Question question = questionWith(QuestionType.ESSAY,
                    "{\"type\":\"ESSAY\",\"task\":\"TASK_2\",\"passBand\":" + band + "}", List.of(), QuestionDifficulty.MEDIUM);
            when(questions.save(question)).thenReturn(question);
            assertThat(publishes(question)).as(band).isTrue();
        }
        for (String band : List.of("3.5", "9.5", "6.25", "\"6.0\"")) {
            assertThat(publishes(questionWith(QuestionType.ESSAY,
                    "{\"type\":\"ESSAY\",\"task\":\"TASK_2\",\"passBand\":" + band + "}", List.of(), QuestionDifficulty.MEDIUM)))
                    .as(band).isFalse();
        }
    }

    @Test
    void taskOneChartFactsHaveOneToTwoThousandCharacters() {
        for (int length : new int[]{1, 2000}) {
            Question question = questionWith(QuestionType.ESSAY, "{\"type\":\"ESSAY\",\"task\":\"TASK_1\",\"passBand\":6.0,"
                    + "\"chartFacts\":\"" + "f".repeat(length) + "\"}", List.of(), QuestionDifficulty.MEDIUM);
            when(questions.save(question)).thenReturn(question);
            assertThat(publishes(question)).as("%d characters", length).isTrue();
        }
        for (int length : new int[]{0, 2001}) {
            assertThat(publishes(questionWith(QuestionType.ESSAY, "{\"type\":\"ESSAY\",\"task\":\"TASK_1\",\"passBand\":6.0,"
                    + "\"chartFacts\":\"" + "f".repeat(length) + "\"}", List.of(), QuestionDifficulty.MEDIUM)))
                    .as("%d characters", length).isFalse();
        }
    }

    @Test
    void anEssaySpecMustMatchTheQuestionType() {
        assertThat(publishes(questionWith(QuestionType.ESSAY, "{\"type\":\"CHOICE\",\"correct\":\"A\"}", AB,
                QuestionDifficulty.MEDIUM))).isFalse();
        assertThat(publishes(questionWith(QuestionType.MULTIPLE_CHOICE, "{\"type\":\"ESSAY\",\"task\":\"TASK_2\",\"passBand\":6.0}",
                AB, QuestionDifficulty.MEDIUM))).isFalse();
        assertThat(publishes(questionWith(QuestionType.ESSAY, "{\"type\":\"ESSAY\",\"task\":\"TASK_3\",\"passBand\":6.0}",
                List.of(), QuestionDifficulty.MEDIUM))).isFalse();
    }
}
