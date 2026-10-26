package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.usecase.*;
import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.exception.DuplicateLessonResourceOrderException;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import com.ieltspath.content.domain.vo.LessonResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LessonResourceAdminControllerTest {
    private final LessonResourceRepository repository = mock(LessonResourceRepository.class);
    private final LearningContentReader lessons = mock(LearningContentReader.class);
    private final UUID lessonId = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new LessonResourceAdminController(
                        new ListLessonResourcesUseCase(repository, lessons),
                        new CreateLessonResourceUseCase(repository, lessons),
                        new UpdateLessonResourceUseCase(repository),
                        new DeleteLessonResourceUseCase(repository),
                        new UpdateLessonSummaryUseCase(repository, lessons),
                        new ReorderLessonResourcesUseCase(repository)))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
        when(lessons.lessonExists(lessonId)).thenReturn(true);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void listsTheSummaryAndSplitsSlidesFromDocumentsInOrder() throws Exception {
        when(repository.findSummaryMarkdown(lessonId)).thenReturn("## Learning Outcomes");
        when(repository.findByLessonId(lessonId)).thenReturn(List.of(
                resource(LessonResourceType.DOCUMENT, "Giáo trình", 0),
                resource(LessonResourceType.DOCUMENT, "Bài tập", 1),
                resource(LessonResourceType.SLIDE, "Slide", 0)));

        mvc.perform(get("/api/content/admin/lessons/{id}/resources", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.summaryMarkdown").value("## Learning Outcomes"))
                .andExpect(jsonPath("$.slides.length()").value(1))
                .andExpect(jsonPath("$.slides[0].title").value("Slide"))
                .andExpect(jsonPath("$.documents[0].title").value("Giáo trình"))
                .andExpect(jsonPath("$.documents[1].sortOrder").value(1));
    }

    @Test
    void unknownLessonsReturnNotFoundForEveryWrite() throws Exception {
        UUID unknown = UUID.randomUUID();
        mvc.perform(get("/api/content/admin/lessons/{id}/resources", unknown)).andExpect(status().isNotFound());
        mvc.perform(post("/api/content/admin/lessons/{id}/resources", unknown).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SLIDE\",\"title\":\"Slide\",\"url\":\"https://a.com\",\"sortOrder\":0}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/content/admin/lessons/{id}/summary", unknown).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"markdown\":\"## Hi\"}"))
                .andExpect(status().isNotFound());
        verify(repository, never()).save(any());
        verify(repository, never()).updateSummaryMarkdown(any(), any());
    }

    @Test
    void createsAResourceWithItsTypeAndTrimmedFields() throws Exception {
        mvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DOCUMENT\",\"title\":\" Giáo trình \",\"url\":\"https://drive.google.com/x\",\"sortOrder\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DOCUMENT"))
                .andExpect(jsonPath("$.title").value("Giáo trình"))
                .andExpect(jsonPath("$.lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.sortOrder").value(2));
    }

    @Test
    void invalidRequestsReturnBadRequestWithoutSaving() throws Exception {
        for (String body : List.of(
                "{\"type\":\"VIDEO\",\"title\":\"x\",\"url\":\"https://a.com\",\"sortOrder\":0}",
                "{\"title\":\"x\",\"url\":\"https://a.com\",\"sortOrder\":0}",
                "{\"type\":\"SLIDE\",\"title\":\" \",\"url\":\"https://a.com\",\"sortOrder\":0}",
                "{\"type\":\"SLIDE\",\"title\":\"x\",\"url\":\"javascript:alert(1)\",\"sortOrder\":0}",
                "{\"type\":\"SLIDE\",\"title\":\"x\",\"url\":\"https://a.com\",\"sortOrder\":-1}",
                "{\"type\":\"SLIDE\",\"title\":\"x\",\"url\":\"https://a.com\"}")) {
            mvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(repository, never()).save(any());
    }

    @Test
    void aTakenOrderReturnsConflict() throws Exception {
        when(repository.save(any())).thenThrow(new DuplicateLessonResourceOrderException(LessonResourceType.SLIDE, 0));
        mvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SLIDE\",\"title\":\"Slide\",\"url\":\"https://a.com\",\"sortOrder\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.code").value("DUPLICATE_RESOURCE_ORDER"));
    }

    @Test
    void updatesAndDeletesOnlyAResourceOfThePathLesson() throws Exception {
        LessonResource own = resource(LessonResourceType.SLIDE, "Slide", 0);
        LessonResource other = LessonResource.create(UUID.randomUUID(), LessonResourceType.SLIDE, "Other",
                "https://a.com", 0);
        when(repository.findById(own.getId())).thenReturn(Optional.of(own));
        when(repository.findById(other.getId())).thenReturn(Optional.of(other));

        mvc.perform(put("/api/content/admin/lessons/{id}/resources/{rid}", lessonId, own.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Slide mới\",\"url\":\"https://b.com\",\"sortOrder\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Slide mới"))
                .andExpect(jsonPath("$.type").value("SLIDE"))
                .andExpect(jsonPath("$.sortOrder").value(3));
        mvc.perform(put("/api/content/admin/lessons/{id}/resources/{rid}", lessonId, other.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"url\":\"https://b.com\",\"sortOrder\":0}"))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/content/admin/lessons/{id}/resources/{rid}", lessonId, other.getId()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/content/admin/lessons/{id}/resources/{rid}", lessonId, own.getId()))
                .andExpect(status().isNoContent());
        verify(repository).delete(own);
        verify(repository, never()).delete(other);
    }

    @Test
    void summaryIsStoredStrippedAndBlankRemovesIt() throws Exception {
        mvc.perform(put("/api/content/admin/lessons/{id}/summary", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"markdown\":\"\\n## Outcomes\\n\"}"))
                .andExpect(status().isNoContent());
        verify(repository).updateSummaryMarkdown(lessonId, "## Outcomes");

        mvc.perform(put("/api/content/admin/lessons/{id}/summary", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"markdown\":\"  \"}"))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/content/admin/lessons/{id}/summary", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
        verify(repository, times(2)).updateSummaryMarkdown(lessonId, null);

        mvc.perform(put("/api/content/admin/lessons/{id}/summary", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"markdown\":\"" + "x".repeat(20_001) + "\"}"))
                .andExpect(status().isBadRequest());
        // The oversized summary never reached the repository: only the three accepted writes did.
        verify(repository, times(3)).updateSummaryMarkdown(eq(lessonId), any());
    }

    private LessonResource resource(LessonResourceType type, String title, int order) {
        return LessonResource.create(lessonId, type, title, "https://example.com/" + order, order);
    }
}
