package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.command.PracticeSetDraftRequest;
import com.ieltspath.content.application.usecase.RequestPracticeSetDraftUseCase;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The practice-set request route: what it hands the use case and how an invalid body is refused. */
class AiPracticeSetDraftControllerTest {

    private final RequestPracticeSetDraftUseCase request = mock(RequestPracticeSetDraftUseCase.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final UUID author = UUID.randomUUID();
    private final UUID point = UUID.randomUUID();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(currentUser.requireUserId()).thenReturn(author);
        mockMvc = MockMvcBuilders.standaloneSetup(new AiPracticeSetDraftController(request, currentUser, new ObjectMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void acceptsTheRequestAndReturnsTheQueuedDraft() throws Exception {
        AiDraft queued = AiDraft.request(DraftKind.PRACTICE_SET, author, "{}", Instant.parse("2026-10-28T00:00:00Z"));
        when(request.execute(eq(author), any())).thenReturn(queued);

        mockMvc.perform(post("/api/content/admin/ai-drafts/practice-sets").contentType("application/json")
                        .content("{\"knowledgePointIds\":[\"" + point + "\"],\"lessonTitle\":\"Bài 1\","
                                + "\"vocabulary\":[\"topic\",\"context\"],"
                                + "\"questionType\":\"MULTIPLE_CHOICE\",\"count\":5,\"difficulty\":\"MEDIUM\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.kind").value("PRACTICE_SET"))
                .andExpect(jsonPath("$.status").value("QUEUED"));

        verify(request).execute(author, new PracticeSetDraftRequest(List.of(point), "Bài 1",
                List.of("topic", "context"), QuestionType.MULTIPLE_CHOICE, 5, QuestionDifficulty.MEDIUM, null));
    }

    @Test
    void aBodyWithoutPointsOrTypeIsBadRequestAndNothingIsQueued() throws Exception {
        mockMvc.perform(post("/api/content/admin/ai-drafts/practice-sets").contentType("application/json")
                        .content("{\"knowledgePointIds\":[],\"count\":5,\"difficulty\":\"MEDIUM\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(request);
    }
}
