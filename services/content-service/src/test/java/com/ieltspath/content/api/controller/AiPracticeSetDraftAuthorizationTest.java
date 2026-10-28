package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.application.usecase.RequestPracticeSetDraftUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiPracticeSetDraftController.class, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class AiPracticeSetDraftAuthorizationTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean RequestPracticeSetDraftUseCase request;
    @MockitoBean CurrentUserProvider currentUser;

    @Test
    @WithMockUser(roles = {"CUSTOMER", "EXAMINER", "ADMIN"})
    void onlyAContentAuthorMayAskForAPracticeSet() throws Exception {
        mockMvc.perform(post("/api/content/admin/ai-drafts/practice-sets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"knowledgePointIds\":[\"" + UUID.randomUUID() + "\"],\"questionType\":\"MULTIPLE_CHOICE\","
                                + "\"count\":5,\"difficulty\":\"MEDIUM\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(request);
    }
}
