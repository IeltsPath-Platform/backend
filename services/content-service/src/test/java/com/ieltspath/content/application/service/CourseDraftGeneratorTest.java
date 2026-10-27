package com.ieltspath.content.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.port.ContentGenerationPort;
import com.ieltspath.content.application.port.CourseCandidateReader;
import com.ieltspath.content.application.port.CourseCandidateReader.PracticeSetRef;
import com.ieltspath.content.application.result.CourseCandidates;
import com.ieltspath.content.application.result.CourseCandidates.Candidate;
import com.ieltspath.content.application.result.GenerationOutcome;
import com.ieltspath.content.application.result.GenerationReply;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.CourseCatalogFacts;
import com.ieltspath.content.domain.vo.CourseCatalogFacts.KnowledgePointFact;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseDraftGeneratorTest {

    private final ObjectMapper json = new ObjectMapper();
    private final CourseCandidateSelector selector = mock(CourseCandidateSelector.class);
    private final CourseCandidateReader candidateReader = mock(CourseCandidateReader.class);
    private final ContentGenerationPort model = mock(ContentGenerationPort.class);

    private final UUID paraphrase = UUID.randomUUID();
    private final UUID setOne = UUID.randomUUID();
    private final UUID question = UUID.randomUUID();

    /** A catalogue that knows the one candidate point and stocks it with a single practice set. */
    private final CourseDraftAnalysisService analysis = new CourseDraftAnalysisService(query -> {
        Map<UUID, KnowledgePointFact> known = new HashMap<>();
        query.knowledgePointIds().forEach(id -> known.put(id, new KnowledgePointFact(id, "R_PARA", "ACTIVE")));
        Map<UUID, Integer> sets = new HashMap<>();
        query.knowledgePointIds().forEach(id -> sets.put(id, 1));
        Map<UUID, CourseCatalogFacts.PackageFact> packages = new HashMap<>();
        query.practiceSetPackageIds().forEach(id -> packages.put(id,
                new CourseCatalogFacts.PackageFact(id, "PS-1", "PRACTICE_SET", "PUBLISHED", null, null, null)));
        return new CourseCatalogFacts(known, new HashSet<>(query.knowledgePointIds()), Map.of(), Map.of(), packages,
                java.util.Set.of(), sets, java.util.Set.of(), Map.of(question.toString(), question), java.util.Set.of(),
                java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), false, java.util.Set.of());
    });

    private final CourseDraftGenerator generator = new CourseDraftGenerator(selector, candidateReader, model,
            new CourseDraftValidator(), new CourseDraftCodec(json), analysis, json);

    private final CourseDraftRequest request = new CourseDraftRequest(new BigDecimal("6.5"), List.of(Skill.READING),
            List.of("paraphrase"), List.of(), 3, "focus on short texts");

    private static final String REPLY = """
            Here is the plan:
            ```json
            {"course":{"code":"R65","name":"Reading 6.5"},
             "topics":[{"code":"T1","name":"Paraphrase","skill":"READING","lessons":[
                {"code":"L1","title":"Paraphrase","knowledgePoints":["R_PARA","R_INVENTED"],
                 "practiceSets":["PS-1"],"questions":["Q1"]}],
               "topicTest":{"code":"T1-TEST","title":"Test"}}],
             "courseTest":{"code":"R65-TEST","title":"Final"}}
            ```
            """;

    private AiDraft draft() throws Exception {
        return AiDraft.request(DraftKind.COURSE, UUID.randomUUID(), json.writeValueAsString(request), Instant.now());
    }

    private void candidates() {
        when(selector.select(org.mockito.ArgumentMatchers.any())).thenReturn(new CourseCandidates(List.of(
                new Candidate(paraphrase, "R_PARA", "Paraphrase", "Rewrite", false,
                        List.of(new PracticeSetRef(setOne, "PS-1")), List.of(question))), List.of("Graphs")));
        when(candidateReader.highestRootTopicSortOrder()).thenReturn(1000);
    }

    @Test
    void theResultHoldsThePlanItsWarningsItsAnalysisAndItsGapHints() throws Exception {
        candidates();
        when(model.generate(anyString())).thenReturn(new GenerationReply(REPLY, "test-model"));

        GenerationOutcome outcome = generator.generate(draft());

        assertEquals("test-model", outcome.model());
        assertEquals("COURSE-v2", outcome.promptVersion());
        JsonNode result = json.readTree(outcome.resultJson());
        assertEquals("R65", result.path("course").path("code").asText());
        assertEquals(1001, result.path("topics").get(0).path("sortOrder").asInt());
        JsonNode lesson = result.path("topics").get(0).path("lessons").get(0);
        assertEquals(paraphrase.toString(), lesson.path("knowledgePointIds").get(0).asText());
        assertEquals(setOne.toString(), lesson.path("practiceSetPackageIds").get(0).asText());
        assertEquals("EXERCISE", lesson.path("blocks").get(0).path("type").asText());
        assertEquals("Graphs", result.path("unmatchedTopics").get(0).asText());
        assertEquals("HALLUCINATED_REFERENCE", result.path("warnings").get(0).path("code").asText());
        assertEquals("R_INVENTED", result.path("warnings").get(0).path("subject").asText());
        assertTrue(result.path("analysis").path("valid").asBoolean());
        assertEquals(1, result.path("analysis").path("coverage").path("covered").asInt());
        // One practice set of the three wanted: a gap, with a hint to ask for questions.
        assertEquals("FEW_PRACTICE_SETS", result.path("gaps").get(0).path("reason").asText());
        assertEquals(paraphrase.toString(), result.path("gapHints").get(0).path("knowledgePointId").asText());
        assertEquals("REQUEST_QUESTION_DRAFT", result.path("gapHints").get(0).path("action").asText());
    }

    @Test
    void noQuestionIsGeneratedToFillAShortage() throws Exception {
        candidates();
        when(model.generate(anyString())).thenReturn(new GenerationReply(REPLY, "m"));

        generator.generate(draft());

        verify(model, times(1)).generate(anyString());
    }

    @Test
    void anUnreadableReplyIsTriedOnceMoreThenAccepted() throws Exception {
        candidates();
        when(model.generate(anyString())).thenReturn(new GenerationReply("I cannot do that.", "m"),
                new GenerationReply(REPLY, "m"));

        GenerationOutcome outcome = generator.generate(draft());

        verify(model, times(2)).generate(anyString());
        assertFalse(outcome.resultJson().isEmpty());
    }

    @Test
    void aReplyThatIsStillUnusableOnTheSecondTryFailsWithInvalidReply() throws Exception {
        candidates();
        when(model.generate(anyString())).thenReturn(new GenerationReply("nope", "m"),
                new GenerationReply("{\"topics\":[]}", "m"));

        ContentGenerationException failure = assertThrows(ContentGenerationException.class,
                () -> generator.generate(draft()));

        assertEquals(ContentGenerationException.INVALID_REPLY, failure.code());
        verify(model, times(2)).generate(anyString());
    }

    @Test
    void aRequestThatMatchesNothingFailsBeforeTheModelIsAsked() throws Exception {
        when(selector.select(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CourseCandidates(List.of(), List.of("Graphs")));

        ContentGenerationException failure = assertThrows(ContentGenerationException.class,
                () -> generator.generate(draft()));

        assertEquals(ContentGenerationException.NO_CANDIDATES, failure.code());
        verify(model, never()).generate(anyString());
    }

    @Test
    void theModelIsOnlyGivenTheCandidateSetAndTheRequest() throws Exception {
        candidates();
        when(model.generate(anyString())).thenReturn(new GenerationReply(REPLY, "m"));
        org.mockito.ArgumentCaptor<String> prompt = org.mockito.ArgumentCaptor.forClass(String.class);

        generator.generate(draft());

        verify(model).generate(prompt.capture());
        assertTrue(prompt.getValue().contains("R_PARA"));
        assertTrue(prompt.getValue().contains("PS-1"));
        assertTrue(prompt.getValue().contains("Q1"));
        assertTrue(prompt.getValue().contains("focus on short texts"));
        assertFalse(prompt.getValue().contains(question.toString()), "question ids are not sent, only labels");
        assertFalse(prompt.getValue().contains(setOne.toString()));
    }
}
