package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.application.usecase.RequestCourseDraftUseCase;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.AiQuotaExceededException;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiCourseDraftControllerTest {

    private final RequestCourseDraftUseCase request = mock(RequestCourseDraftUseCase.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final UUID author = UUID.randomUUID();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(currentUser.requireUserId()).thenReturn(author);
        mockMvc = MockMvcBuilders.standaloneSetup(new AiCourseDraftController(request, currentUser,
                new ObjectMapper())).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void aRequestIsAcceptedAndTheQueuedDraftReturned() throws Exception {
        AiDraft queued = AiDraft.request(DraftKind.COURSE, author, "{\"bandLevel\":6.5}", Instant.now());
        when(request.execute(eq(author), any())).thenReturn(queued);

        mockMvc.perform(post("/api/content/admin/ai-drafts/courses").contentType("application/json").content(
                        "{\"bandLevel\":6.5,\"skills\":[\"READING\"],\"topics\":[\"Paraphrase\"],\"lessonCount\":6,"
                                + "\"instructions\":\"short\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.kind").value("COURSE"))
                .andExpect(jsonPath("$.status").value("QUEUED"));

        ArgumentCaptor<CourseDraftRequest> sent = ArgumentCaptor.forClass(CourseDraftRequest.class);
        verify(request).execute(eq(author), sent.capture());
        assertEquals(new BigDecimal("6.5"), sent.getValue().bandLevel());
        assertEquals(List.of(Skill.READING), sent.getValue().skills());
        assertEquals(List.of("Paraphrase"), sent.getValue().topics());
        assertEquals(6, sent.getValue().lessons());
        assertEquals("short", sent.getValue().instructions());
    }

    @Test
    void theBandIsRequired() throws Exception {
        mockMvc.perform(post("/api/content/admin/ai-drafts/courses").contentType("application/json")
                        .content("{\"skills\":[\"READING\"],\"topics\":[\"a\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownSkillIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/content/admin/ai-drafts/courses").contentType("application/json")
                        .content("{\"bandLevel\":6.5,\"skills\":[\"DANCING\"],\"topics\":[\"a\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anExhaustedAllowanceIsTooManyRequests() throws Exception {
        when(request.execute(eq(author), any())).thenThrow(new AiQuotaExceededException());

        mockMvc.perform(post("/api/content/admin/ai-drafts/courses").contentType("application/json")
                        .content("{\"bandLevel\":6.5,\"skills\":[\"READING\"],\"topics\":[\"a\"]}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.details.code").value("AI_QUOTA_EXCEEDED"));
    }

    @Test
    void aRefusedRequestIsABadRequest() throws Exception {
        when(request.execute(eq(author), any())).thenThrow(new IllegalArgumentException("topics or knowledgePointIds"));

        mockMvc.perform(post("/api/content/admin/ai-drafts/courses").contentType("application/json")
                        .content("{\"bandLevel\":6.5,\"skills\":[\"READING\"]}"))
                .andExpect(status().isBadRequest());
    }
}
