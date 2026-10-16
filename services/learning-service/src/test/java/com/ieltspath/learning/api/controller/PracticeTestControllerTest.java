package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.result.TestAssignmentResult;
import com.ieltspath.learning.application.usecase.AssignPracticeTestUseCase;
import com.ieltspath.learning.application.usecase.ListPracticeTestsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=PracticeTestController.class,properties={"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class PracticeTestControllerTest {
    static final byte[] KEY=new byte[32];
    static { new SecureRandom().nextBytes(KEY); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.auth.internal-jwt-secret",()->Base64.getEncoder().encodeToString(KEY));
    }
    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean ListPracticeTestsUseCase list;
    @MockitoBean AssignPracticeTestUseCase assign;
    final UUID learner=UUID.randomUUID(),course=UUID.randomUUID(),pkg=UUID.randomUUID();
    String path() { return "/api/learning/courses/"+course+"/practice-tests"; }

    @Test void listingUsesVerifiedIdentity() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(list.execute(learner,course)).thenReturn(List.of());
        mvc.perform(get(path()).with(user("customer").roles("CUSTOMER")).header("X-User-Id",UUID.randomUUID()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(list).execute(learner,course);
    }
    @Test void assignmentReturnsTheVersionForAssessment() throws Exception {
        UUID version=UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(learner);
        when(assign.execute(learner,course,pkg)).thenReturn(new TestAssignmentResult(UUID.randomUUID(),pkg,version));
        mvc.perform(post(path()+"/"+pkg+"/assignments").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.packageVersionId").value(version.toString()));
        verify(assign).execute(learner,course,pkg);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","CONTENT_AUTHOR","EXAMINER","SALES_STAFF"})
    void onlyCustomerCanUseLearnerRoutes(String role) throws Exception {
        mvc.perform(get(path()).with(user("staff").roles(role))).andExpect(status().isForbidden());
        mvc.perform(post(path()+"/"+pkg+"/assignments").with(user("staff").roles(role))).andExpect(status().isForbidden());
        verifyNoInteractions(list,assign,currentUser);
    }
    @Test void unauthenticatedCallerCannotReadMilestones() throws Exception {
        mvc.perform(get(path())).andExpect(status().isUnauthorized());
    }
}
