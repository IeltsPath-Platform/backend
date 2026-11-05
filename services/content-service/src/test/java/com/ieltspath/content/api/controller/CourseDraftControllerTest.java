package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.usecase.ApproveCourseDraftUseCase;
import com.ieltspath.content.application.usecase.CourseDraftUseCases;
import com.ieltspath.content.application.usecase.FindPracticeSetsForPointsUseCase;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.CourseDraftErrorsException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.InvalidCourseDraftException;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis.Issue;
import com.ieltspath.content.domain.vo.DraftKind;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The course draft routes: response shapes and the status each failure maps to. Security is tested separately. */
class CourseDraftControllerTest {

    private final CourseDraftUseCases drafts = mock(CourseDraftUseCases.class);
    private final ApproveCourseDraftUseCase approve = mock(ApproveCourseDraftUseCase.class);
    private final FindPracticeSetsForPointsUseCase practiceSets = mock(FindPracticeSetsForPointsUseCase.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final UUID author = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(currentUser.requireUserId()).thenReturn(author);
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseDraftController(drafts, approve, practiceSets,
                currentUser, new ObjectMapper())).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private AiDraft ready() {
        return AiDraft.manual(DraftKind.COURSE, author, "{\"course\":null,\"topics\":[]}", Instant.parse("2026-10-23T00:00:00Z"));
    }

    @Test
    void practiceSetsForPointsListsThePackageIdsThatFit() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID package1 = UUID.randomUUID();
        when(practiceSets.execute(List.of(first, second))).thenReturn(java.util.Set.of(package1));

        mockMvc.perform(get("/api/content/admin/course-drafts/practice-sets")
                        .param("knowledgePointIds", first + "," + second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageIds[0]").value(package1.toString()));
    }

    @Test
    void practiceSetsForNoPointsIsAnEmptyList() throws Exception {
        when(practiceSets.execute(List.of())).thenReturn(java.util.Set.of());

        mockMvc.perform(get("/api/content/admin/course-drafts/practice-sets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageIds").isEmpty());
    }

    @Test
    void createReturnsTheDraft() throws Exception {
        when(drafts.create(eq(author), any())).thenReturn(ready());

        mockMvc.perform(post("/api/content/admin/course-drafts").contentType("application/json")
                        .content("{\"structure\":{\"topics\":[]}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("COURSE"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.result.topics").isArray())
                .andExpect(jsonPath("$.model").isEmpty());
    }

    @Test
    void createWithoutABodyMakesAnEmptyDraft() throws Exception {
        when(drafts.create(eq(author), any())).thenReturn(ready());

        mockMvc.perform(post("/api/content/admin/course-drafts")).andExpect(status().isCreated());
        verify(drafts).create(author, null);
    }

    @Test
    void anInvalidStructureIsBadRequestWithItsIssues() throws Exception {
        when(drafts.create(eq(author), any())).thenThrow(new InvalidCourseDraftException(List.of(
                new InvalidCourseDraftException.Problem("course.code", "REQUIRED"))));

        mockMvc.perform(post("/api/content/admin/course-drafts").contentType("application/json")
                        .content("{\"structure\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("INVALID_COURSE_DRAFT"))
                .andExpect(jsonPath("$.details.issues[0].path").value("course.code"))
                .andExpect(jsonPath("$.details.issues[0].problem").value("REQUIRED"));
    }

    @Test
    void saveNeedsAStructure() throws Exception {
        mockMvc.perform(put("/api/content/admin/course-drafts/{id}", id).contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveReturnsTheUpdatedDraftAndRefusesADraftNotReady() throws Exception {
        when(drafts.save(eq(author), eq(id), any())).thenReturn(ready());
        mockMvc.perform(put("/api/content/admin/course-drafts/{id}", id).contentType("application/json")
                        .content("{\"structure\":{\"topics\":[]}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READY"));

        when(drafts.save(eq(author), eq(id), any())).thenThrow(new InvalidContentStateException("Draft is APPROVED"));
        mockMvc.perform(put("/api/content/admin/course-drafts/{id}", id).contentType("application/json")
                        .content("{\"structure\":{\"topics\":[]}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analysisHasTheDocumentedShape() throws Exception {
        UUID point = UUID.randomUUID();
        when(drafts.analyze(author, id)).thenReturn(new CourseDraftAnalysis(
                List.of(new Issue("PREREQUISITE_ORDER", point.toString(), "other")),
                List.of(new Issue("KP_NOT_COVERED", point.toString(), null)),
                List.of(new CourseDraftAnalysis.Gap(point, "FEW_PRACTICE_SETS")),
                new CourseDraftAnalysis.Coverage(4, 3)));

        mockMvc.perform(get("/api/content/admin/course-drafts/{id}/analysis", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("PREREQUISITE_ORDER"))
                .andExpect(jsonPath("$.errors[0].related").value("other"))
                .andExpect(jsonPath("$.warnings[0].code").value("KP_NOT_COVERED"))
                .andExpect(jsonPath("$.warnings[0].related").isEmpty())
                .andExpect(jsonPath("$.gaps[0].knowledgePointId").value(point.toString()))
                .andExpect(jsonPath("$.gaps[0].reason").value("FEW_PRACTICE_SETS"))
                .andExpect(jsonPath("$.coverage.expected").value(4))
                .andExpect(jsonPath("$.coverage.covered").value(3));
    }

    @Test
    void approveReturnsWhatItCreated() throws Exception {
        UUID course = UUID.randomUUID();
        UUID topic = UUID.randomUUID();
        UUID lesson = UUID.randomUUID();
        UUID test = UUID.randomUUID();
        when(approve.execute(author, id)).thenReturn(new ApproveCourseDraftUseCase.Approved(id, course,
                List.of(topic), List.of(lesson), List.of(test)));

        mockMvc.perform(post("/api/content/admin/course-drafts/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftId").value(id.toString()))
                .andExpect(jsonPath("$.courseId").value(course.toString()))
                .andExpect(jsonPath("$.topicIds[0]").value(topic.toString()))
                .andExpect(jsonPath("$.lessonIds[0]").value(lesson.toString()))
                .andExpect(jsonPath("$.finalTestPackageIds[0]").value(test.toString()));
    }

    @Test
    void takenNamesAreAConflictWithTheirList() throws Exception {
        when(approve.execute(author, id)).thenThrow(new CourseDraftErrorsException(
                List.of(new Issue("CODE_TAKEN", "R65_L1", "LESSON")), true));

        mockMvc.perform(post("/api/content/admin/course-drafts/{id}/approve", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.code").value("CODE_CONFLICT"))
                .andExpect(jsonPath("$.details.conflicts[0].code").value("CODE_TAKEN"))
                .andExpect(jsonPath("$.details.conflicts[0].subject").value("R65_L1"))
                .andExpect(jsonPath("$.details.conflicts[0].related").value("LESSON"));
    }

    @Test
    void otherErrorsAreUnprocessableWithTheirList() throws Exception {
        when(approve.execute(author, id)).thenThrow(new CourseDraftErrorsException(
                List.of(new Issue("PREREQUISITE_ORDER", "a", "b")), false));

        mockMvc.perform(post("/api/content/admin/course-drafts/{id}/approve", id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.code").value("COURSE_DRAFT_INVALID"))
                .andExpect(jsonPath("$.details.errors[0].code").value("PREREQUISITE_ORDER"));
    }
}
