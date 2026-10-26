package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestCourseDraftUseCaseTest {

    private final StartAiDraftUseCase start = mock(StartAiDraftUseCase.class);
    private final ContentAiProperties properties = new ContentAiProperties();
    private final RequestCourseDraftUseCase useCase = new RequestCourseDraftUseCase(start, new ObjectMapper(), properties);
    private final UUID author = UUID.randomUUID();

    private static CourseDraftRequest request(String band, List<Skill> skills, List<String> topics, List<UUID> ids,
                                              Integer lessons, String instructions) {
        return new CourseDraftRequest(band == null ? null : new BigDecimal(band), skills, topics, ids, lessons,
                instructions);
    }

    private static CourseDraftRequest good() {
        return request("6.5", List.of(Skill.READING), List.of("Paraphrase"), List.of(), 8, null);
    }

    private void refused(CourseDraftRequest request) {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(author, request));
        verify(start, never()).execute(any(), any(), any(), anyInt());
    }

    @Test
    void aValidRequestIsQueuedAgainstTheAuthorsAllowance() {
        AiDraft queued = AiDraft.request(DraftKind.COURSE, author, "{}", Instant.now());
        when(start.execute(eq(author), eq(DraftKind.COURSE), any(), eq(20))).thenReturn(queued);

        AiDraft result = useCase.execute(author, good());

        assertEquals(queued, result);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(start).execute(eq(author), eq(DraftKind.COURSE), body.capture(), eq(20));
        assertTrue(body.getValue().contains("\"bandLevel\":6.5"));
        assertTrue(body.getValue().contains("Paraphrase"));
    }

    @Test
    void theBandMustBeAHalfBandInRange() {
        refused(request(null, List.of(Skill.READING), List.of("a"), List.of(), 8, null));
        refused(request("6.3", List.of(Skill.READING), List.of("a"), List.of(), 8, null));
        refused(request("9.5", List.of(Skill.READING), List.of("a"), List.of(), 8, null));
        refused(request("-1", List.of(Skill.READING), List.of("a"), List.of(), 8, null));
    }

    @Test
    void atLeastOneSkillAndNoRepeats() {
        refused(request("6.5", List.of(), List.of("a"), List.of(), 8, null));
        refused(request("6.5", List.of(Skill.READING, Skill.READING), List.of("a"), List.of(), 8, null));
    }

    @Test
    void topicsOrKnowledgePointsAreRequired() {
        refused(request("6.5", List.of(Skill.READING), List.of(), List.of(), 8, null));
    }

    @Test
    void knowledgePointIdsAloneAreEnough() {
        when(start.execute(any(), any(), any(), anyInt()))
                .thenReturn(AiDraft.request(DraftKind.COURSE, author, "{}", Instant.now()));

        useCase.execute(author, request("6.5", List.of(Skill.READING), List.of(), List.of(UUID.randomUUID()), null,
                null));

        verify(start).execute(any(), any(), any(), anyInt());
    }

    @Test
    void topicPhrasesAreLimitedInCountAndLength() {
        refused(request("6.5", List.of(Skill.READING), Collections.nCopies(11, "a"), List.of(), 8, null));
        refused(request("6.5", List.of(Skill.READING), List.of("x".repeat(101)), List.of(), 8, null));
        refused(request("6.5", List.of(Skill.READING), List.of("  "), List.of(), 8, null));
    }

    @Test
    void theKnowledgePointListHonoursTheCandidateLimit() {
        properties.getAi().setMaxCourseCandidates(2);

        refused(request("6.5", List.of(Skill.READING), List.of(), List.of(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID()), 8, null));
    }

    @Test
    void lessonCountAndInstructionsAreBounded() {
        refused(request("6.5", List.of(Skill.READING), List.of("a"), List.of(), 0, null));
        refused(request("6.5", List.of(Skill.READING), List.of("a"), List.of(), 31, null));
        refused(request("6.5", List.of(Skill.READING), List.of("a"), List.of(), 8, "x".repeat(501)));
    }
}
