package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.GetGameContentSnapshotCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class GetGameContentSnapshotUseCaseTest {
    private final QuestionRepository repository = mock(QuestionRepository.class);
    private final LearningContentReader reader = mock(LearningContentReader.class);
    private final GetGameContentSnapshotUseCase useCase = new GetGameContentSnapshotUseCase(repository, reader);

    @Test
    void refusesVocabularyDomainWithoutReadingContentDatabase() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GetGameContentSnapshotCommand("SPELLING", "VOCABULARY", List.of(UUID.randomUUID()))));
        verifyNoInteractions(repository, reader);
    }

    @Test
    void refusesQuestionsUsedByLessonsPracticeSetsOrTopicTests() {
        Question question = publishedQuestion();
        when(repository.findByIds(List.of(question.getId()))).thenReturn(List.of(question));
        when(reader.questionVersionsReservedForLearning(anyCollection()))
                .thenReturn(Set.of(question.getCurrentPublishedVersionId()));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GetGameContentSnapshotCommand("SENTENCE_COMPLETION", "GRAMMAR", List.of(question.getId()))));
    }

    @Test
    void returnsGrammarQuestionsThatNoLearningContentUses() {
        Question question = publishedQuestion();
        when(repository.findByIds(List.of(question.getId()))).thenReturn(List.of(question));
        when(reader.questionVersionsReservedForLearning(Set.of(question.getCurrentPublishedVersionId())))
                .thenReturn(Set.of());

        var result = useCase.execute(
                new GetGameContentSnapshotCommand("SENTENCE_COMPLETION", "GRAMMAR", List.of(question.getId())));

        assertThat(result.items()).extracting(item -> item.questionVersionId())
                .containsExactly(question.getCurrentPublishedVersionId());
    }

    private static Question publishedQuestion() {
        Question question = Question.create(QuestionType.FILL_IN_BLANK, Skill.WRITING, null);
        QuestionVersion version = QuestionVersion.create(question.getId(), 1, "She ___ to school.", null,
                "{\"type\":\"FILL\",\"accepted\":[\"goes\"]}", null, null);
        version.publish();
        question.addVersion(version);
        question.publishVersion(version.getId());
        return question;
    }
}
