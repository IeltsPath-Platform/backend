package com.group01.content.api.controller;

import com.group01.content.api.exception.GlobalExceptionHandler;
import com.group01.content.application.result.KnowledgePointResult;
import com.group01.content.application.usecase.CreateKnowledgePointUseCase;
import com.group01.content.application.usecase.GetKnowledgePointsUseCase;
import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.KnowledgePointKind;
import com.group01.content.domain.vo.LearningType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KnowledgePointControllerTest {
    @Mock private GetKnowledgePointsUseCase getKnowledgePointsUseCase;
    @Mock private CreateKnowledgePointUseCase createKnowledgePointUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new KnowledgePointController(getKnowledgePointsUseCase, createKnowledgePointUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void returnsAllCanonicalLearningTypesAndPreservesKnowledgePointUuid() throws Exception {
        UUID id = UUID.randomUUID();
        List<KnowledgePointResult> results = List.of(
                result(id, LearningType.MEMORY),
                result(UUID.randomUUID(), LearningType.CONCEPT),
                result(UUID.randomUUID(), LearningType.PROCEDURE),
                result(UUID.randomUUID(), LearningType.DESIGN)
        );
        when(getKnowledgePointsUseCase.execute(null)).thenReturn(results);

        mockMvc.perform(get("/api/content/knowledge-points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].learningType").value("MEMORY"))
                .andExpect(jsonPath("$[1].learningType").value("CONCEPT"))
                .andExpect(jsonPath("$[2].learningType").value("PROCEDURE"))
                .andExpect(jsonPath("$[3].learningType").value("DESIGN"));
    }

    @Test
    void rejectsMissingLearningTypeOnKnowledgePointCreation() throws Exception {
        mockMvc.perform(post("/api/content/knowledge-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"%s","code":"KP-1","name":"Example","kind":"GRAMMAR"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verify(createKnowledgePointUseCase, never()).execute(any());
    }

    @Test
    void rejectsNonCanonicalLearningType() throws Exception {
        mockMvc.perform(post("/api/content/knowledge-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"%s","code":"KP-1","name":"Example","kind":"GRAMMAR","learningType":"GRAMMAR"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verify(createKnowledgePointUseCase, never()).execute(any());
    }

    private KnowledgePointResult result(UUID id, LearningType learningType) {
        Instant now = Instant.parse("2026-09-24T00:00:00Z");
        return new KnowledgePointResult(
                id,
                UUID.randomUUID(),
                "KP-CODE",
                "Knowledge point",
                KnowledgePointKind.GRAMMAR,
                learningType,
                null,
                null,
                ContentStatus.ACTIVE,
                now,
                now
        );
    }
}
