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
        when(work.list(any(), any(), any())).thenReturn(List.of());
        mvc.perform(get("/api/learning/admin/work-items").with(user("staff").roles(role))).andExpect(status().isOk());
        Set<Queue> expected = new java.util.HashSet<>();
        for (String queue : queues.split("\\+")) expected.add(Queue.valueOf(queue));
        verify(work).list(eq(expected), eq(null), eq(null));
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
