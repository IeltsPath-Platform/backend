package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.result.LessonAuthoringResult;
import com.ieltspath.content.application.usecase.LessonAuthoringUseCase;
import com.ieltspath.content.domain.exception.*;
import com.ieltspath.content.domain.vo.LessonStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LessonAuthoringAdminController.class)
@Import({CommonSecurityAutoConfiguration.class, com.ieltspath.content.api.exception.GlobalExceptionHandler.class})
class LessonAuthoringAdminControllerTest {
    private static final byte[] KEY = new byte[32];
    static { java.util.Arrays.fill(KEY, (byte) 7); }
    @org.springframework.test.context.DynamicPropertySource
    static void auth(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> java.util.Base64.getEncoder().encodeToString(KEY));
        registry.add("app.auth.internal-jwt-issuer", () -> "urn:code-base:api-gateway");
    }
    @Autowired MockMvc mvc;
    @MockitoBean LessonAuthoringUseCase authoring;
    private final UUID lessonId = UUID.randomUUID();

    @Test @WithMockUser(roles="CUSTOMER") void customerIsForbidden() throws Exception {
        mvc.perform(post("/api/content/admin/lessons/{id}/blocks", lessonId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"blockType\":\"TEXT\",\"textContent\":\"text\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(authoring);
    }

    @Test @WithMockUser(roles="CONTENT_AUTHOR") void contentAuthorCanCreateLesson() throws Exception {
        LessonAuthoringResult result = new LessonAuthoringResult(lessonId, UUID.randomUUID(), "L", "Lesson", null,
                null, null, 0, LessonStatus.DRAFT, Instant.now(), Instant.now(), List.of(), List.of());
        when(authoring.create(any(), eq("L"), eq("Lesson"), any(), any(), any(), any())).thenReturn(result);
        mvc.perform(post("/api/content/admin/topics/{topicId}/lessons", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"L\",\"title\":\"Lesson\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(lessonId.toString()));
    }

    @Test @WithMockUser(roles="CONTENT_AUTHOR") void exposesStableAuthoringErrorCodes() throws Exception {
        when(authoring.addBlock(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new LessonBlockInvalidException("Invalid block fields"));
        when(authoring.publish(any())).thenThrow(new LessonNotPublishableException(List.of("LESSON_HAS_NO_ACTIVE_KP")));
        doThrow(new LessonExerciseLockedException()).when(authoring).deleteBlock(any(), any());
        doThrow(new LessonSortConflictException()).when(authoring).reorderBlocks(any(), any());

        mvc.perform(post("/api/content/admin/lessons/{id}/blocks", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockType\":\"TEXT\",\"textContent\":\"text\"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.details.code").value("LESSON_BLOCK_INVALID"));
        mvc.perform(post("/api/content/admin/lessons/{id}/publish", lessonId))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.details.code").value("LESSON_NOT_PUBLISHABLE"));
        mvc.perform(delete("/api/content/admin/lessons/{id}/blocks/{blockId}", lessonId, UUID.randomUUID()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.details.code").value("LESSON_EXERCISE_LOCKED"));
        mvc.perform(put("/api/content/admin/lessons/{id}/blocks/order", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.details.code").value("LESSON_SORT_CONFLICT"));
    }
}
