package com.ieltspath.content.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.QuestionDraftRequest;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.port.ContentGenerationPort;
import com.ieltspath.content.application.result.GenerationOutcome;
import com.ieltspath.content.application.result.GenerationReply;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionDraftGeneratorTest {

    private final ObjectMapper json = new ObjectMapper();
    private final UUID kpId = UUID.randomUUID();

    @Test
    void keepsUsableItemsAndRecordsWhyTheOthersWereDropped() throws Exception {
        String reply = "{\"items\":["
                + "{\"stem\":\"Q1\",\"explanation\":\"E\",\"options\":[{\"optionKey\":\"A\",\"content\":\"x\"},"
                + "{\"optionKey\":\"B\",\"content\":\"y\"}],\"correct\":\"A\"},"
                + "{\"stem\":\"Q2\",\"explanation\":\"E\",\"correct\":\"Z\",\"options\":[{\"optionKey\":\"A\",\"content\":\"x\"},"
                + "{\"optionKey\":\"B\",\"content\":\"y\"}]}]}";
        GenerationOutcome outcome = generator(reply).generate(draft(QuestionType.MULTIPLE_CHOICE, 2));

        JsonNode result = json.readTree(outcome.resultJson());
        assertEquals(1, result.path("items").size());
        assertEquals("CORRECT_NOT_AN_OPTION", result.path("rejected").get(0).path("reason").asText());
        assertEquals("CHOICE", result.path("items").get(0).path("answerSpec").path("type").asText());
        assertEquals("QUESTIONS-v1", outcome.promptVersion());
    }

    @Test
    void replyWithoutAnyUsableItemFailsTheDraft() throws Exception {
        ContentGenerationException failure = assertThrows(ContentGenerationException.class,
                () -> generator("{\"items\":[{\"stem\":\"\"}]}").generate(draft(QuestionType.MULTIPLE_CHOICE, 1)));
        assertEquals(ContentGenerationException.INVALID_REPLY, failure.code());
    }

    @Test
    void replyThatIsNotJsonFailsWithoutEchoingIt() throws Exception {
        ContentGenerationException failure = assertThrows(ContentGenerationException.class,
                () -> generator("no json here").generate(draft(QuestionType.MULTIPLE_CHOICE, 1)));
        assertEquals(ContentGenerationException.INVALID_REPLY, failure.code());
        assertFalse(failure.getMessage().contains("no json"));
    }

    @Test
    void dropsAStemTheKnowledgePointHasAndOneRepeatedInTheSameReply() throws Exception {
        String reply = "{\"items\":["
                + item("Already   in the bank") + "," + item("Fresh one") + "," + item("FRESH ONE") + "]}";

        GenerationOutcome outcome = generator(reply, "already in the bank").generate(
                draft(QuestionType.MULTIPLE_CHOICE, 5));

        JsonNode result = json.readTree(outcome.resultJson());
        assertEquals(1, result.path("items").size());
        assertEquals("Fresh one", result.path("items").get(0).path("stem").asText());
        assertEquals(List.of("DUPLICATE_STEM", "DUPLICATE_STEM"), List.of(
                result.path("rejected").get(0).path("reason").asText(),
                result.path("rejected").get(1).path("reason").asText()));
    }

    private static String item(String stem) {
        return "{\"stem\":\"" + stem + "\",\"explanation\":\"E\",\"options\":[{\"optionKey\":\"A\",\"content\":\"x\"},"
                + "{\"optionKey\":\"B\",\"content\":\"y\"}],\"correct\":\"A\"}";
    }

    private QuestionDraftGenerator generator(String modelReply, String... existingStems) {
        ContentGenerationPort model = prompt -> new GenerationReply(modelReply, "m-1");
        KnowledgePointRepository points = mock(KnowledgePointRepository.class);
        KnowledgePoint point = mock(KnowledgePoint.class);
        when(point.getStatus()).thenReturn(ContentStatus.ACTIVE);
        when(point.getName()).thenReturn("Paraphrase");
        when(points.findById(kpId)).thenReturn(Optional.of(point));
        ErrorTypeRepository errorTypes = mock(ErrorTypeRepository.class);
        when(errorTypes.findApprovedActive()).thenReturn(List.of());
        com.ieltspath.content.application.port.QuestionStemReader stems = id -> List.of(existingStems);
        return new QuestionDraftGenerator(model, points, errorTypes, stems, json, new ContentAiProperties());
    }

    private AiDraft draft(QuestionType type, int count) throws Exception {
        QuestionDraftRequest request = new QuestionDraftRequest(kpId, type, count, QuestionDifficulty.MEDIUM, null);
        return new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.GENERATING, UUID.randomUUID(),
                json.writeValueAsString(request), null, null, null, null, Instant.now(), Instant.now(), null, null,
                null, null);
    }
}
