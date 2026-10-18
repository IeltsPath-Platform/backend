package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.usecase.ListLessonResourcesUseCase;
import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import com.ieltspath.content.domain.vo.LessonResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalLessonResourceControllerTest {
    private final LessonResourceRepository repository = mock(LessonResourceRepository.class);
    private final LearningContentReader lessons = mock(LearningContentReader.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new InternalLessonResourceController(
                        new ListLessonResourcesUseCase(repository, lessons)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void publishedLessonReturnsSummarySlidesAndDocuments() throws Exception {
        UUID lessonId = UUID.randomUUID();
        when(lessons.publishedLessonExists(lessonId)).thenReturn(true);
        when(repository.findSummaryMarkdown(lessonId)).thenReturn("## Outcomes");
        when(repository.findByLessonId(lessonId)).thenReturn(List.of(
                LessonResource.create(lessonId, LessonResourceType.DOCUMENT, "Giáo trình", "https://d.com/1", 0),
                LessonResource.create(lessonId, LessonResourceType.SLIDE, "Slide", "https://docs.google.com/presentation/d/x", 0)));

        mvc.perform(get("/internal/learning-content/lessons/{id}/resources", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryMarkdown").value("## Outcomes"))
                .andExpect(jsonPath("$.slides[0].url").value("https://docs.google.com/presentation/d/x"))
                .andExpect(jsonPath("$.documents[0].title").value("Giáo trình"))
                .andExpect(jsonPath("$.documents[0].sortOrder").value(0));
    }

    @Test
    void lessonWithoutResourcesReturnsNullSummaryAndEmptyLists() throws Exception {
        UUID lessonId = UUID.randomUUID();
        when(lessons.publishedLessonExists(lessonId)).thenReturn(true);
        when(repository.findByLessonId(lessonId)).thenReturn(List.of());

        mvc.perform(get("/internal/learning-content/lessons/{id}/resources", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryMarkdown").doesNotExist())
                .andExpect(jsonPath("$.slides").isEmpty())
                .andExpect(jsonPath("$.documents").isEmpty());
    }

    @Test
    void draftOrUnknownLessonIsNotFound() throws Exception {
        UUID draft = UUID.randomUUID();
        when(lessons.lessonExists(draft)).thenReturn(true);
        when(lessons.publishedLessonExists(draft)).thenReturn(false);

        mvc.perform(get("/internal/learning-content/lessons/{id}/resources", draft)).andExpect(status().isNotFound());
        verify(repository, never()).findByLessonId(draft);
    }
}
