package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.command.UpdateRuleConfigCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.RuleConfigResult;
import com.ieltspath.learning.application.usecase.ManageRuleConfigUseCase;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RuleConfigController.class, properties = {"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class RuleConfigControllerTest {
    static final byte[] KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    private static final String PATH = "/api/learning/admin/rule-configs";
    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean ManageRuleConfigUseCase rules;
    final UUID admin = UUID.randomUUID();
    final RuleConfigResult current = new RuleConfigResult(0.6, 0.8, List.of(7, 14, 30, 45, 60), 3, List.of());

    @Test
    void anAdminReadsTheRulesInForce() throws Exception {
        when(rules.get()).thenReturn(current);

        mvc.perform(get(PATH).with(user("author").roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priorityBelow").value(0.6))
                .andExpect(jsonPath("$.reviewIntervalsDays[2]").value(30))
                .andExpect(jsonPath("$.consolidateDays").value(3));
    }

    @Test
    void anUpdateIsAttributedToTheVerifiedAdmin() throws Exception {
        when(currentUser.requireUserId()).thenReturn(admin);
        var command = new UpdateRuleConfigCommand(0.7, null, null, 4);
        when(rules.update(admin, command)).thenReturn(current);

        mvc.perform(put(PATH).with(user("author").roles("CONTENT_AUTHOR")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priorityBelow\":0.7,\"consolidateDays\":4}"))
                .andExpect(status().isOk());
        verify(rules).update(admin, command);
    }

    @Test
    void anInvalidRuleSetComesBackAs422WithItsCode() throws Exception {
        when(currentUser.requireUserId()).thenReturn(admin);
        when(rules.update(admin, new UpdateRuleConfigCommand(0.9, null, null, null))).thenThrow(
                new LearningRequestException(422, "INVALID_RULE_CONFIG", "Thresholds must satisfy 0 < priorityBelow"));

        mvc.perform(put(PATH).with(user("author").roles("CONTENT_AUTHOR")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priorityBelow\":0.9}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_RULE_CONFIG"));
    }

    @Test
    void recomputeAnswers202WhenStartedAnd409WhenAJobIsAlreadyRunning() throws Exception {
        when(rules.recompute()).thenReturn(true, false);

        mvc.perform(post(PATH + "/recompute").with(user("author").roles("CONTENT_AUTHOR")))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("STARTED"));
        mvc.perform(post(PATH + "/recompute").with(user("author").roles("CONTENT_AUTHOR")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value("ALREADY_RUNNING"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "CUSTOMER", "EXAMINER", "SALES_STAFF"})
    void onlyAnAdminCanReadOrChangeTheRules(String role) throws Exception {
        mvc.perform(get(PATH).with(user("other").roles(role))).andExpect(status().isForbidden());
        mvc.perform(put(PATH).with(user("other").roles(role)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(PATH + "/recompute").with(user("other").roles(role))).andExpect(status().isForbidden());
        verifyNoInteractions(rules);
    }

    @Test
    void unauthenticatedCallersAreRejected() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }
}
