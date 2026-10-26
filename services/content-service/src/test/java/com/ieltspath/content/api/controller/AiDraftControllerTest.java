package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.usecase.AiDraftReviewUseCases;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.AiDraftNotFoundException;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiDraftControllerTest {

    private final UUID author = UUID.randomUUID();
    private final AiDraftReviewUseCases drafts = mock(AiDraftReviewUseCases.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(currentUser.requireUserId()).thenReturn(author);
        mockMvc = MockMvcBuilders.standaloneSetup(new AiDraftController(drafts, currentUser, new ObjectMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listReturnsOnlyTheRequestersDraftsWithParsedJson() throws Exception {
        AiDraft ready = new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.READY, author,
                "{\"count\":5}", "{\"items\":[]}", null, "m", "p1", Instant.now(), null, Instant.now(), null, null, null);
        when(drafts.list(eq(author), any(), any(), anyInt(), anyInt())).thenReturn(List.of(ready));

        mockMvc.perform(get("/api/content/admin/ai-drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("READY"))
                .andExpect(jsonPath("$[0].request.count").value(5))
                .andExpect(jsonPath("$[0].result.items").isArray());
    }

    @Test
    void anotherAuthorsDraftIsNotFound() throws Exception {
        UUID someoneElses = UUID.randomUUID();
        when(drafts.get(author, someoneElses)).thenThrow(new AiDraftNotFoundException(someoneElses));

        mockMvc.perform(get("/api/content/admin/ai-drafts/{id}", someoneElses))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectPassesTheNoteThrough() throws Exception {
        UUID id = UUID.randomUUID();
        AiDraft rejected = new AiDraft(id, DraftKind.QUESTIONS, DraftStatus.REJECTED, author, "{}", "{}", null, null,
                null, Instant.now(), null, Instant.now(), author, Instant.now(), "off topic");
        when(drafts.reject(eq(author), eq(id), eq("off topic"))).thenReturn(rejected);

        mockMvc.perform(post("/api/content/admin/ai-drafts/{id}/reject", id)
                        .contentType("application/json").content("{\"note\":\"off topic\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
