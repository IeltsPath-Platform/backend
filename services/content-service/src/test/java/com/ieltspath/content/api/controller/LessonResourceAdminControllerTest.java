package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.port.LessonDocumentFiles;
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
    private final LessonDocumentFiles files = mock(LessonDocumentFiles.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new LessonResourceAdminController(
                        new ListLessonResourcesUseCase(repository, lessons, files),
                        new CreateLessonResourceUseCase(repository, lessons, files),
                        new UpdateLessonResourceUseCase(repository, files),
                        new DeleteLessonResourceUseCase(repository, files),
                        new UpdateLessonSummaryUseCase(repository, lessons),
                        new ReorderLessonResourcesUseCase(repository),
                        currentUser))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
        when(lessons.lessonExists(lessonId)).thenReturn(true);
        when(currentUser.requireUserId()).thenReturn(userId);
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

    private String ownKey() {
        return "lesson-document/" + userId + "/" + UUID.randomUUID() + ".pdf";
    }

    private String fileBody(String key, String type) {
        return "{\"type\":\"" + type + "\",\"title\":\"Giáo trình\",\"fileKey\":\"" + key + "\",\"sortOrder\":0}";
    }

    @Test
    void attachesAnUploadedPdfAndSignsItsLink() throws Exception {
        String key = ownKey();
        when(files.exists(key)).thenReturn(true);
        when(files.signedUrl(key)).thenReturn("https://storage.example/signed?token=t");

        mvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content(fileBody(key, "DOCUMENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DOCUMENT"))
                .andExpect(jsonPath("$.fileKey").value(key))
                .andExpect(jsonPath("$.url").value("https://storage.example/signed?token=t"));
        verify(repository).save(argThat(saved -> key.equals(saved.getFileKey()) && saved.getUrl() == null));
    }

    @Test
    void rejectsAKeyThatAnotherAuthorUploadedWithoutSaving() throws Exception {
        String foreign = "lesson-document/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".pdf";
        when(files.exists(foreign)).thenReturn(true);

        mvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content(fileBody(foreign, "DOCUMENT")))
                .andExpect(status().isBadRequest());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsKeysOutsideTheDocumentFolderAndPathTricks() throws Exception {
        for (String key : List.of(
                "content-image/" + userId + "/" + UUID.randomUUID() + ".png",
                "lesson-document/" + userId + "/../" + UUID.randomUUID() + ".pdf",
                "lesson-document/" + userId + "/not-a-uuid.pdf")) {
            when(files.exists(key)).thenReturn(true);
            mvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId)
                            .contentType(MediaType.APPLICATION_JSON).content(fileBody(key, "DOCUMENT")))
                    .andExpect(status().isBadRequest());
        }
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsAKeyWhoseFileIsNotStored() throws Exception {
        String key = ownKey();
        when(files.exists(key)).thenReturn(false);

        mvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content(fileBody(key, "DOCUMENT")))
                .andExpect(status().isBadRequest());
        verify(repository, never()).save(any());
    }

    @Test
    void aSlideCannotBeAnUploadedFileAndALinkAndAFileCannotBeBoth() throws Exception {
        String key = ownKey();
        when(files.exists(key)).thenReturn(true);

        mvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content(fileBody(key, "SLIDE")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DOCUMENT\",\"title\":\"x\",\"url\":\"https://a.com\",\"fileKey\":\"" + key
                                + "\",\"sortOrder\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DOCUMENT\",\"title\":\"x\",\"sortOrder\":0}"))
                .andExpect(status().isBadRequest());
        verify(repository, never()).save(any());
    }

    @Test
    void listSignsEachUploadedFileAndLeavesLinksAlone() throws Exception {
        String key = ownKey();
        when(files.signedUrl(key)).thenReturn("https://storage.example/signed?token=t");
        when(repository.findByLessonId(lessonId)).thenReturn(List.of(
                LessonResource.createFile(lessonId, "Giáo trình", key, 0),
                resource(LessonResourceType.DOCUMENT, "Liên kết", 1)));

        mvc.perform(get("/api/content/admin/lessons/{id}/resources", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[0].url").value("https://storage.example/signed?token=t"))
                .andExpect(jsonPath("$.documents[0].fileKey").value(key))
                .andExpect(jsonPath("$.documents[1].url").value("https://example.com/1"))
                .andExpect(jsonPath("$.documents[1].fileKey").doesNotExist());
    }

    @Test
    void renamingAFileResourceKeepsItsFileAndRefusesALink() throws Exception {
        String key = ownKey();
        LessonResource file = LessonResource.createFile(lessonId, "Cũ", key, 0);
        when(repository.findById(file.getId())).thenReturn(Optional.of(file));
        when(files.signedUrl(key)).thenReturn("https://storage.example/signed?token=t");

        mvc.perform(put("/api/content/admin/lessons/{id}/resources/{rid}", lessonId, file.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Mới\",\"sortOrder\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Mới"))
                .andExpect(jsonPath("$.fileKey").value(key));
        mvc.perform(put("/api/content/admin/lessons/{id}/resources/{rid}", lessonId, file.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mới\",\"url\":\"https://a.com\",\"sortOrder\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingAFileResourceRemovesItsFileAndAFailedCleanupDoesNotFailTheDelete() throws Exception {
        String key = ownKey();
        LessonResource file = LessonResource.createFile(lessonId, "Giáo trình", key, 0);
        when(repository.findById(file.getId())).thenReturn(Optional.of(file));
        doThrow(new IllegalStateException("storage down")).when(files).delete(key);

        mvc.perform(delete("/api/content/admin/lessons/{id}/resources/{rid}", lessonId, file.getId()))
                .andExpect(status().isNoContent());
        verify(repository).delete(file);
        verify(files).delete(key);
    }

    private LessonResource resource(LessonResourceType type, String title, int order) {
        return LessonResource.create(lessonId, type, title, "https://example.com/" + order, order);
    }
}
