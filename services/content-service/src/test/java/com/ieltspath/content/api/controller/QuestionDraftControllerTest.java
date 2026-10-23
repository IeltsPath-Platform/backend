package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.command.QuestionDraftItemPatch;
import com.ieltspath.content.application.usecase.ApproveQuestionDraftUseCase;
import com.ieltspath.content.application.usecase.EditDraftItemUseCase;
import com.ieltspath.content.application.usecase.RequestQuestionDraftUseCase;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.DraftItemNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.InvalidDraftItemException;
import com.ieltspath.content.domain.vo.DraftKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The question draft routes: response shapes and the status each failure maps to. Security is tested separately. */
class QuestionDraftControllerTest {

    private final RequestQuestionDraftUseCase request = mock(RequestQuestionDraftUseCase.class);
    private final ApproveQuestionDraftUseCase approve = mock(ApproveQuestionDraftUseCase.class);
    private final EditDraftItemUseCase edit = mock(EditDraftItemUseCase.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final UUID author = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(currentUser.requireUserId()).thenReturn(author);
        mockMvc = MockMvcBuilders.standaloneSetup(new QuestionDraftController(request, approve, edit, currentUser,
                new ObjectMapper())).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private AiDraft ready() {
        return AiDraft.manual(DraftKind.QUESTIONS, author, "{\"items\":[{\"stem\":\"Q\"}],\"rejected\":[]}",
                Instant.parse("2026-10-23T00:00:00Z"));
    }

    @Test
    void anEditPassesOnlyTheFieldsGivenAndReturnsTheDraft() throws Exception {
        when(edit.edit(eq(author), eq(id), eq(1), any())).thenReturn(ready());

        mockMvc.perform(patch("/api/content/admin/ai-drafts/{id}/items/{index}", id, 1)
                        .contentType("application/json")
                        .content("{\"stem\":\"New stem\",\"correct\":\"B\",\"distractors\":{\"A\":\"PARAPHRASE_TRAP\"},"
                                + "\"options\":[{\"optionKey\":\"A\",\"content\":\"x\"},{\"optionKey\":\"B\",\"content\":\"y\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("QUESTIONS"))
                .andExpect(jsonPath("$.result.items[0].stem").value("Q"));

        ArgumentCaptor<QuestionDraftItemPatch> patch = ArgumentCaptor.forClass(QuestionDraftItemPatch.class);
        verify(edit).edit(eq(author), eq(id), eq(1), patch.capture());
        assertEquals("New stem", patch.getValue().stem());
        assertEquals(null, patch.getValue().explanation());
        assertEquals("B", patch.getValue().correct());
        assertEquals(Map.of("A", "PARAPHRASE_TRAP"), patch.getValue().distractors());
        assertEquals(List.of(new QuestionDraftItemPatch.Option("A", "x"), new QuestionDraftItemPatch.Option("B", "y")),
                patch.getValue().options());
        assertEquals(null, patch.getValue().accepted());
    }

    @Test
    void anEditThatBreaksARuleIsUnprocessableWithItsReason() throws Exception {
        when(edit.edit(eq(author), eq(id), eq(0), any())).thenThrow(new InvalidDraftItemException("DUPLICATE_STEM"));

        mockMvc.perform(patch("/api/content/admin/ai-drafts/{id}/items/{index}", id, 0)
                        .contentType("application/json").content("{\"stem\":\"Same\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.code").value("INVALID_DRAFT_ITEM"))
                .andExpect(jsonPath("$.details.reason").value("DUPLICATE_STEM"));
    }

    @Test
    void anOversizeEditIsBadRequestAndNothingIsEdited() throws Exception {
        mockMvc.perform(patch("/api/content/admin/ai-drafts/{id}/items/{index}", id, 0)
                        .contentType("application/json").content("{\"stem\":\"" + "s".repeat(4001) + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/content/admin/ai-drafts/{id}/items/{index}", id, 0)
                        .contentType("application/json")
                        .content("{\"options\":[{\"optionKey\":null,\"content\":\"x\"}]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(edit);
    }

    @Test
    void aMissingItemIsNotFoundAndADraftNotReadyIsBadRequest() throws Exception {
        when(edit.remove(author, id, 9)).thenThrow(new DraftItemNotFoundException(9));
        when(edit.remove(author, id, 0)).thenThrow(new InvalidContentStateException("AI draft is APPROVED"));

        mockMvc.perform(delete("/api/content/admin/ai-drafts/{id}/items/{index}", id, 9))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/content/admin/ai-drafts/{id}/items/{index}", id, 0))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removingAnItemReturnsTheDraft() throws Exception {
        when(edit.remove(author, id, 0)).thenReturn(ready());

        mockMvc.perform(delete("/api/content/admin/ai-drafts/{id}/items/{index}", id, 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void approveReturnsTheApprovedDraft() throws Exception {
        AiDraft draft = ready();
        draft.approve(author, Instant.parse("2026-10-23T01:00:00Z"));
        when(approve.execute(eq(author), eq(id), eq(List.of(0, 2)))).thenReturn(draft);

        mockMvc.perform(post("/api/content/admin/ai-drafts/{id}/approve", id).contentType("application/json")
                        .content("{\"itemIndexes\":[0,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approvingNoItemIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/content/admin/ai-drafts/{id}/approve", id).contentType("application/json")
                        .content("{\"itemIndexes\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(approve);
    }
}
