package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.PracticeSetDraftRequest;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestPracticeSetDraftUseCaseTest {

    private final KnowledgePointRepository points = mock(KnowledgePointRepository.class);
    private final StartAiDraftUseCase start = mock(StartAiDraftUseCase.class);
    private final ContentAiProperties properties = new ContentAiProperties();
    private final RequestPracticeSetDraftUseCase useCase = new RequestPracticeSetDraftUseCase(points, start,
            new ObjectMapper(), properties);
    private final UUID author = UUID.randomUUID();
    private final UUID reading = readingPoint();

    @Test
    void queuesAPracticeSetDraftWithEachPointOnce() {
        AiDraft queued = mock(AiDraft.class);
        when(start.execute(eq(author), eq(DraftKind.PRACTICE_SET), any(), eq(properties.getAi().getDailyLimit()))).thenReturn(queued);

        AiDraft result = useCase.execute(author, request(List.of(reading, reading), QuestionType.MULTIPLE_CHOICE, 5));

        assertSame(queued, result);
        verify(start).execute(eq(author), eq(DraftKind.PRACTICE_SET), org.mockito.ArgumentMatchers.argThat(
                body -> body.contains("\"count\":5") && body.indexOf(reading.toString()) == body.lastIndexOf(reading.toString())),
                anyInt());
    }

    @Test
    void refusesAPointThatIsNotReadingBecauseTheModelCannotWriteAudio() {
        UUID listening = point(Skill.LISTENING, ContentStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(author, request(List.of(listening), QuestionType.MULTIPLE_CHOICE, 5)));
        verify(start, never()).execute(any(), any(), any(), anyInt());
    }

    @Test
    void refusesAnUnknownOrInactivePoint() {
        UUID unknown = UUID.randomUUID();
        when(points.findById(unknown)).thenReturn(Optional.empty());
        UUID inactive = point(Skill.READING, ContentStatus.INACTIVE);

        assertThrows(KnowledgePointNotFoundException.class,
                () -> useCase.execute(author, request(List.of(unknown), QuestionType.MULTIPLE_CHOICE, 5)));
        assertThrows(InvalidContentStateException.class,
                () -> useCase.execute(author, request(List.of(inactive), QuestionType.MULTIPLE_CHOICE, 5)));
    }

    @Test
    void refusesNoPointsTooManyPointsAndAnUnsupportedShape() {
        List<UUID> tooMany = IntStream.rangeClosed(0, RequestPracticeSetDraftUseCase.MAX_POINTS)
                .mapToObj(index -> readingPoint()).toList();

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(author, request(List.of(), QuestionType.MULTIPLE_CHOICE, 5)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(author, request(tooMany, QuestionType.MULTIPLE_CHOICE, 5)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(author, request(List.of(reading), QuestionType.SHORT_ANSWER, 5)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(author, request(List.of(reading), QuestionType.MULTIPLE_CHOICE, 2)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(author, request(List.of(reading), QuestionType.MULTIPLE_CHOICE, 11)));
        verify(start, never()).execute(any(), any(), any(), anyInt());
    }

    @Test
    void refusesTooLongInstructionsAndAMissingDifficulty() {
        String longText = "x".repeat(RequestPracticeSetDraftUseCase.MAX_INSTRUCTIONS + 1);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(author, new PracticeSetDraftRequest(List.of(reading),
                "Bài 1", List.of(), QuestionType.MULTIPLE_CHOICE, 5, QuestionDifficulty.MEDIUM, longText)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(author, new PracticeSetDraftRequest(List.of(reading),
                "Bài 1", List.of(), QuestionType.MULTIPLE_CHOICE, 5, null, null)));
        verify(start, never()).execute(any(), any(), any(), anyInt());
    }

    @Test
    void keepsTheLessonWordsOnceEachAndWithoutBlanks() {
        AiDraft queued = mock(AiDraft.class);
        when(start.execute(eq(author), eq(DraftKind.PRACTICE_SET), any(), anyInt())).thenReturn(queued);

        useCase.execute(author, requestWith(List.of(" topic ", "topic", "", "context")));

        verify(start).execute(eq(author), eq(DraftKind.PRACTICE_SET), org.mockito.ArgumentMatchers.argThat(
                body -> body.contains("\"vocabulary\":[\"topic\",\"context\"]")), anyInt());
    }

    @Test
    void refusesTooManyWordsAndAWordThatIsTooLong() {
        List<String> tooMany = IntStream.rangeClosed(0, RequestPracticeSetDraftUseCase.MAX_VOCABULARY)
                .mapToObj(index -> "word" + index).toList();

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(author, requestWith(tooMany)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(author,
                requestWith(List.of("x".repeat(RequestPracticeSetDraftUseCase.MAX_WORD + 1)))));
        verify(start, never()).execute(any(), any(), any(), anyInt());
    }

    private PracticeSetDraftRequest requestWith(List<String> words) {
        return new PracticeSetDraftRequest(List.of(reading), "Bài 1", words, QuestionType.MULTIPLE_CHOICE, 5,
                QuestionDifficulty.MEDIUM, null);
    }

    private PracticeSetDraftRequest request(List<UUID> ids, QuestionType type, int count) {
        return new PracticeSetDraftRequest(ids, "Bài 1", List.of(), type, count, QuestionDifficulty.MEDIUM, null);
    }

    private UUID readingPoint() {
        return point(Skill.READING, ContentStatus.ACTIVE);
    }

    private UUID point(Skill skill, ContentStatus status) {
        UUID id = UUID.randomUUID();
        KnowledgePoint point = mock(KnowledgePoint.class);
        when(point.getStatus()).thenReturn(status);
        when(point.getSkill()).thenReturn(skill);
        when(points.findById(id)).thenReturn(Optional.of(point));
        return id;
    }
}
