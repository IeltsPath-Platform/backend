package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.usecase.GetPracticeTestMilestonesUseCase;
import com.ieltspath.content.application.usecase.ReplacePracticeTestMilestonesUseCase;
import com.ieltspath.content.domain.exception.CourseNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.vo.PracticeTestMilestone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PracticeTestMilestoneController.class, InternalPracticeTestMilestoneController.class}, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class PracticeTestMilestoneControllerTest {
    private static final String ADMIN_PATH = "/api/content/admin/courses/{id}/practice-test-milestones";
    private static final String INTERNAL_PATH = "/internal/learning-content/courses/{id}/practice-test-milestones";
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID PACKAGE_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final byte[] INTERNAL_KEY = new byte[32];
    private static final PracticeTestMilestone MILESTONE = new PracticeTestMilestone(
            PACKAGE_ID, VERSION_ID, "PT-MIDDLE", "Mid-course practice test", 2);
    private static final List<PracticeTestMilestone> COMMAND = List.of(
            new PracticeTestMilestone(PACKAGE_ID, null, null, null, 2));

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean GetPracticeTestMilestonesUseCase getMilestones;
    @MockitoBean ReplacePracticeTestMilestonesUseCase replaceMilestones;

    @ParameterizedTest
    @ValueSource(strings = {"CONTENT_AUTHOR"})
    void contentAuthorCanReadAllConfiguredMilestones(String role) throws Exception {
        when(getMilestones.execute(COURSE_ID, false)).thenReturn(List.of(MILESTONE));
        mockMvc.perform(get(ADMIN_PATH, COURSE_ID).with(user("author").roles(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].packageId").value(PACKAGE_ID.toString()))
                .andExpect(jsonPath("$[0].packageVersionId").value(VERSION_ID.toString()))
                .andExpect(jsonPath("$[0].code").value("PT-MIDDLE"))
                .andExpect(jsonPath("$[0].title").value("Mid-course practice test"))
                .andExpect(jsonPath("$[0].afterTopicSequence").value(2));
        verify(getMilestones).execute(COURSE_ID, false);
        verifyNoInteractions(replaceMilestones);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONTENT_AUTHOR"})
    void contentAuthorCanReplaceMilestonesUsingOnlyPackageAndPosition(String role) throws Exception {
        when(replaceMilestones.execute(COURSE_ID, COMMAND)).thenReturn(List.of(MILESTONE));
        mockMvc.perform(put(ADMIN_PATH, COURSE_ID).with(user("author").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].packageId").value(PACKAGE_ID.toString()))
                .andExpect(jsonPath("$[0].packageVersionId").value(VERSION_ID.toString()))
                .andExpect(jsonPath("$[0].code").value("PT-MIDDLE"))
                .andExpect(jsonPath("$[0].title").value("Mid-course practice test"))
                .andExpect(jsonPath("$[0].afterTopicSequence").value(2));
        verify(replaceMilestones).execute(COURSE_ID, COMMAND);
        verifyNoInteractions(getMilestones);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONTENT_AUTHOR"})
    void contentAuthorCanClearMilestonesWithAnEmptyArray(String role) throws Exception {
        when(replaceMilestones.execute(COURSE_ID, List.of())).thenReturn(List.of());
        mockMvc.perform(put(ADMIN_PATH, COURSE_ID).with(user("author").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"milestones\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        verify(replaceMilestones).execute(COURSE_ID, List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "EXAMINER"})
    void customerAndExaminerCannotReadOrReplaceAdminMilestones(String role) throws Exception {
        mockMvc.perform(get(ADMIN_PATH, COURSE_ID).with(user("learner").roles(role)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(ADMIN_PATH, COURSE_ID).with(user("learner").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(getMilestones, replaceMilestones);
    }

    @Test
    void adminCannotReadOrReplaceMilestones() throws Exception {
        mockMvc.perform(get(ADMIN_PATH, COURSE_ID).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(ADMIN_PATH, COURSE_ID).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(getMilestones, replaceMilestones);
    }

    @Test
    void unauthenticatedRequestsCannotReadOrReplaceMilestones() throws Exception {
        mockMvc.perform(get(ADMIN_PATH, COURSE_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(ADMIN_PATH, COURSE_ID).contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(INTERNAL_PATH, COURSE_ID)).andExpect(status().isUnauthorized());
        verifyNoInteractions(getMilestones, replaceMilestones);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"milestones\":null}", "{\"milestones\":[null]}",
            "{\"milestones\":[{\"afterTopicSequence\":1}]}",
            "{\"milestones\":[{\"packageId\":null,\"afterTopicSequence\":1}]}",
            "{\"milestones\":[{\"packageId\":\"not-a-uuid\",\"afterTopicSequence\":1}]}",
            "{\"milestones\":[{\"packageId\":\"10000000-0000-4000-8000-000000000001\"}]}",
            "{\"milestones\":[{\"packageId\":\"10000000-0000-4000-8000-000000000001\",\"afterTopicSequence\":0}]}",
            "{\"milestones\":[{\"packageId\":\"10000000-0000-4000-8000-000000000001\",\"afterTopicSequence\":-1}]}"
    })
    void invalidRequestsAreRejectedBeforeCallingTheUseCase(String body) throws Exception {
        mockMvc.perform(put(ADMIN_PATH, COURSE_ID).with(user("author").roles("CONTENT_AUTHOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(getMilestones, replaceMilestones);
    }

    @Test
    void requestsWithMoreThanOneHundredMilestonesAreRejectedBeforeCallingTheUseCase() throws Exception {
        String entry = "{\"packageId\":\"" + PACKAGE_ID + "\",\"afterTopicSequence\":1}";
        String body = "{\"milestones\":[" + String.join(",", Collections.nCopies(101, entry)) + "]}";
        mockMvc.perform(put(ADMIN_PATH, COURSE_ID).with(user("author").roles("CONTENT_AUTHOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(getMilestones, replaceMilestones);
    }

    @Test
    void invalidPackageOrPositionFromTheUseCaseReturnsBadRequest() throws Exception {
        when(replaceMilestones.execute(COURSE_ID, COMMAND))
                .thenThrow(new InvalidContentStateException("A published practice test and reachable position are required"));
        mockMvc.perform(put(ADMIN_PATH, COURSE_ID).with(user("author").roles("CONTENT_AUTHOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isBadRequest());
        verify(replaceMilestones).execute(COURSE_ID, COMMAND);
    }

    @Test
    void unknownCoursesReturnNotFoundForAdminReadsAndReplacement() throws Exception {
        when(getMilestones.execute(COURSE_ID, false)).thenThrow(new CourseNotFoundException(COURSE_ID));
        when(replaceMilestones.execute(COURSE_ID, COMMAND)).thenThrow(new CourseNotFoundException(COURSE_ID));
        mockMvc.perform(get(ADMIN_PATH, COURSE_ID).with(user("author").roles("CONTENT_AUTHOR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(ADMIN_PATH, COURSE_ID).with(user("author").roles("CONTENT_AUTHOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isNotFound());
    }

    @Test
    void internalReadsRequestOnlyPublishedMilestonesForActiveCourses() throws Exception {
        when(getMilestones.execute(COURSE_ID, true)).thenReturn(List.of(MILESTONE));
        mockMvc.perform(get(INTERNAL_PATH, COURSE_ID).with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].packageId").value(PACKAGE_ID.toString()))
                .andExpect(jsonPath("$[0].packageVersionId").value(VERSION_ID.toString()))
                .andExpect(jsonPath("$[0].afterTopicSequence").value(2));
        verify(getMilestones).execute(COURSE_ID, true);
        verifyNoInteractions(replaceMilestones);
    }

    @Test
    void internalReadsReturnNotFoundForInactiveOrUnknownCourses() throws Exception {
        when(getMilestones.execute(COURSE_ID, true)).thenThrow(new CourseNotFoundException(COURSE_ID));
        mockMvc.perform(get(INTERNAL_PATH, COURSE_ID).with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isNotFound());
        verify(getMilestones).execute(COURSE_ID, true);
    }

    private String validRequest() {
        return "{\"milestones\":[{\"packageId\":\"" + PACKAGE_ID + "\",\"afterTopicSequence\":2}]}";
    }
}
