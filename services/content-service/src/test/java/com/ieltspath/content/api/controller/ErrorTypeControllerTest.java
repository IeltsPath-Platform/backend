package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.usecase.CreateErrorTypeUseCase;
import com.ieltspath.content.application.usecase.ListErrorTypesUseCase;
import com.ieltspath.content.application.usecase.UpdateErrorTypeUseCase;
import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.vo.ErrorCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ErrorTypeController.class, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class ErrorTypeControllerTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean ListErrorTypesUseCase listErrorTypes;
    @MockitoBean CreateErrorTypeUseCase createErrorType;
    @MockitoBean UpdateErrorTypeUseCase updateErrorType;

    @Test
    @WithMockUser(roles = "EXAMINER")
    void examinerCanListErrorTypes() throws Exception {
        when(listErrorTypes.execute(anyBoolean())).thenReturn(List.of(
                ErrorType.create("PARAPHRASE_TRAP", "Paraphrase trap", ErrorCategory.READING_STRATEGY, 2, null, "hint")));

        mockMvc.perform(get("/api/content/error-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PARAPHRASE_TRAP"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotListErrorTypes() throws Exception {
        mockMvc.perform(get("/api/content/error-types")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void examinerCannotCreateErrorTypes() throws Exception {
        mockMvc.perform(post("/api/content/error-types")
                        .contentType("application/json")
                        .content("{\"code\":\"TENSE\",\"name\":\"Tense\",\"category\":\"GRAMMAR\",\"severity\":1,"
                                + "\"fixHint\":\"hint\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void contentAuthorCanCreateAnErrorType() throws Exception {
        when(createErrorType.execute(anyString(), anyString(), any(), anyInt(), isNull(), anyString()))
                .thenReturn(ErrorType.create("TENSE", "Tense", ErrorCategory.GRAMMAR, 1, null, "hint"));

        mockMvc.perform(post("/api/content/error-types")
                        .contentType("application/json")
                        .content("{\"code\":\"TENSE\",\"name\":\"Tense\",\"category\":\"GRAMMAR\",\"severity\":1,"
                                + "\"fixHint\":\"hint\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TENSE"));
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void severityOutsideOneToThreeIsRejected() throws Exception {
        mockMvc.perform(post("/api/content/error-types")
                        .contentType("application/json")
                        .content("{\"code\":\"TENSE\",\"name\":\"Tense\",\"category\":\"GRAMMAR\",\"severity\":4,"
                                + "\"fixHint\":\"hint\"}"))
                .andExpect(status().isBadRequest());
    }
}
