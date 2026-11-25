package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.usecase.StaffInsightUseCase;
import com.ieltspath.learning.application.usecase.StaffWorkUseCase;
import com.ieltspath.learning.domain.vo.StaffRecords.Queue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StaffAdminController.class, properties = {"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class StaffAdminControllerTest {
    static final byte[] KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean StaffInsightUseCase insight;
    @MockitoBean StaffWorkUseCase work;
    final UUID id = UUID.randomUUID();

    @ParameterizedTest
    @CsvSource({"EXAMINER,EXAMINER", "CONTENT_AUTHOR,CONTENT_AUTHOR"})
    void eachRoleListsOnlyTheQueuesItMayWork(String role, String queues) throws Exception {
        when(work.listPageWithSubjects(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new com.ieltspath.learning.application.result.WorkItemPage(List.of(), 0, 25, 0, 0));
        mvc.perform(get("/api/learning/admin/work-items").with(user("staff").roles(role))).andExpect(status().isOk());
        Set<Queue> expected = new java.util.HashSet<>();
        for (String queue : queues.split("\\+")) expected.add(Queue.valueOf(queue));
        verify(work).listPageWithSubjects(eq(expected), eq(null), eq(null), eq(null), eq(false), eq(0), eq(25));
    }

    @Test
    void theKindFilterIsPassedOnAndEachItemCarriesItsSubject() throws Exception {
        var item = new com.ieltspath.learning.domain.vo.StaffRecords.WorkItem(id, Queue.CONTENT_AUTHOR,
                com.ieltspath.learning.domain.vo.StaffRecords.WorkKind.CONTENT_GAP, null, id, "{}",
                com.ieltspath.learning.domain.vo.StaffRecords.WorkStatus.OPEN, null, null, null, null, false,
                java.time.Instant.parse("2026-10-01T00:00:00Z"), java.time.Instant.parse("2026-10-01T00:00:00Z"));
        when(work.listPageWithSubjects(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new com.ieltspath.learning.application.result.WorkItemPage(List.of(
                        new com.ieltspath.learning.application.result.WorkItemView(item,
                                java.util.Map.of("type", "KNOWLEDGE_POINT", "name", "Paraphrase"))), 0, 25, 1, 1));

        mvc.perform(get("/api/learning/admin/work-items?kind=CONTENT_GAP")
                        .with(user("staff").roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.content[0].subject.name").value("Paraphrase"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.content[0].subjectRef").value(id.toString()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.content[0].detail").value("{}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalElements").value(1));
        verify(work).listPageWithSubjects(eq(Set.of(Queue.CONTENT_AUTHOR)), eq(null), eq(null),
                eq(com.ieltspath.learning.domain.vo.StaffRecords.WorkKind.CONTENT_GAP), eq(false), eq(0), eq(25));
    }

    @Test
    void administratorsCannotAccessStaffQueues() throws Exception {
        mvc.perform(get("/api/learning/admin/work-items").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(work, insight);
    }

    @ParameterizedTest
    @CsvSource({"CUSTOMER,/api/learning/admin/work-items", "SALES_STAFF,/api/learning/admin/work-items",
            "CONTENT_AUTHOR,/api/learning/admin/learners/00000000-0000-0000-0000-000000000001/overview",
            "EXAMINER,/api/learning/admin/question-stats", "EXAMINER,/api/learning/admin/tree-quality",
            "CUSTOMER,/api/learning/admin/question-stats"})
    void otherRolesAreRefused(String role, String path) throws Exception {
        mvc.perform(get(path).with(user("other").roles(role))).andExpect(status().isForbidden());
        verifyNoInteractions(work, insight);
    }

    @Test
    void examinersReviewFlagsButContentAuthorsCannot() throws Exception {
        when(currentUser.requireUserId()).thenReturn(UUID.randomUUID());
        String body = "{\"status\":\"DISMISSED\",\"note\":\"fine\"}";
        mvc.perform(patch("/api/learning/admin/flags/" + id).with(user("a").roles("EXAMINER"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mvc.perform(patch("/api/learning/admin/flags/" + id).with(user("b").roles("CONTENT_AUTHOR"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    }

    @Test
    void onlyALearnerReadsTheirNotesAndAnonymousCallersAreRejected() throws Exception {
        when(currentUser.requireUserId()).thenReturn(UUID.randomUUID());
        when(work.notesFor(any())).thenReturn(List.of());
        mvc.perform(get("/api/learning/notes").with(user("l").roles("CUSTOMER"))).andExpect(status().isOk());
        mvc.perform(get("/api/learning/notes").with(user("s").roles("EXAMINER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/learning/notes")).andExpect(status().isUnauthorized());
    }
}
