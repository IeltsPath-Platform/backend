package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.application.command.CreateKnowledgePointCommand;
import com.ieltspath.content.application.command.UpdateKnowledgePointCommand;
import com.ieltspath.content.application.usecase.CreateKnowledgePointUseCase;
import com.ieltspath.content.application.usecase.ActivateKnowledgePointUseCase;
import com.ieltspath.content.application.usecase.DeactivateKnowledgePointUseCase;
import com.ieltspath.content.application.usecase.GetKnowledgePointPrerequisitesUseCase;
import com.ieltspath.content.application.usecase.GetKnowledgePointsUseCase;
import com.ieltspath.content.application.usecase.ReplaceKnowledgePointPrerequisitesUseCase;
import com.ieltspath.content.application.usecase.UpdateKnowledgePointUseCase;
import com.ieltspath.content.domain.exception.InvalidPrerequisiteException;
import com.ieltspath.content.domain.exception.KnowledgePointInUseException;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.exception.PrerequisiteCycleException;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KnowledgePointControllerTest {
    @Mock private GetKnowledgePointsUseCase getKnowledgePointsUseCase;
    @Mock private CreateKnowledgePointUseCase createKnowledgePointUseCase;
    @Mock private UpdateKnowledgePointUseCase updateKnowledgePointUseCase;
    @Mock private DeactivateKnowledgePointUseCase deactivateKnowledgePointUseCase;
    @Mock private ActivateKnowledgePointUseCase activateKnowledgePointUseCase;
    @Mock private GetKnowledgePointPrerequisitesUseCase getKnowledgePointPrerequisitesUseCase;
    @Mock private ReplaceKnowledgePointPrerequisitesUseCase replaceKnowledgePointPrerequisitesUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new KnowledgePointController(getKnowledgePointsUseCase, createKnowledgePointUseCase,
                                updateKnowledgePointUseCase, deactivateKnowledgePointUseCase, activateKnowledgePointUseCase,
                                getKnowledgePointPrerequisitesUseCase, replaceKnowledgePointPrerequisitesUseCase))
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
                .andExpect(jsonPath("$[3].learningType").value("DESIGN"))
                .andExpect(jsonPath("$[0].bandMin").doesNotExist())
                .andExpect(jsonPath("$[0].bandMax").doesNotExist())
                .andExpect(jsonPath("$[0].effectiveBandMin").doesNotExist())
                .andExpect(jsonPath("$[0].effectiveBandMax").doesNotExist());
    }

    @Test
    void exposesNullableCefrAndScopeMetadataForExistingKnowledgePoints() throws Exception {
        when(getKnowledgePointsUseCase.execute(null)).thenReturn(List.of(result(UUID.randomUUID(), LearningType.MEMORY)));

        mockMvc.perform(get("/api/content/knowledge-points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cefrLevel").hasJsonPath())
                .andExpect(jsonPath("$[0].canDo").hasJsonPath())
                .andExpect(jsonPath("$[0].scopeIn").hasJsonPath())
                .andExpect(jsonPath("$[0].scopeOut").hasJsonPath());
    }

    @Test
    void returnsCefrAndScopeMetadataWhenPresent() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-17T00:00:00Z");
        when(getKnowledgePointsUseCase.execute(null)).thenReturn(List.of(new KnowledgePointResult(id,
                UUID.randomUUID(), "G55_COMPLEX_SENTENCES", "Complex sentences", KnowledgePointKind.GRAMMAR,
                LearningType.CONCEPT, com.ieltspath.content.domain.vo.Skill.ALL, "Legacy description",
                ContentStatus.ACTIVE, now, now, CefrLevel.B1, "Can form a clear complex sentence.",
                "Basic subordinate clauses", "Advanced clause reduction")));

        mockMvc.perform(get("/api/content/knowledge-points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cefrLevel").value("B1"))
                .andExpect(jsonPath("$[0].canDo").value("Can form a clear complex sentence."))
                .andExpect(jsonPath("$[0].scopeIn").value("Basic subordinate clauses"))
                .andExpect(jsonPath("$[0].scopeOut").value("Advanced clause reduction"));
    }

    @Test
    void acceptsGrammarKnowledgePointCodeAndAllSkill() throws Exception {
        UUID id = UUID.randomUUID();
        when(createKnowledgePointUseCase.execute(any())).thenReturn(result(id, LearningType.CONCEPT));

        mockMvc.perform(post("/api/content/knowledge-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"%s","code":"G55_COMPLEX_SENTENCES","name":"Complex sentences",
                                 "kind":"GRAMMAR","learningType":"CONCEPT","skill":"ALL"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());

        verify(createKnowledgePointUseCase).execute(any());
    }

    @Test
    void forwardsCefrCanDoAndScopeFieldsWhenCreatingKnowledgePoints() throws Exception {
        when(createKnowledgePointUseCase.execute(any())).thenReturn(result(UUID.randomUUID(), LearningType.CONCEPT));

        mockMvc.perform(post("/api/content/knowledge-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"%s","code":"G60_PERFECT_TENSES","name":"Perfect tenses",
                                 "kind":"GRAMMAR","learningType":"CONCEPT","skill":"ALL",
                                 "cefrLevel":"B1","canDo":"Connect events to a reference time.",
                                 "scopeIn":"Present perfect","scopeOut":"Perfect continuous forms"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());

        verify(createKnowledgePointUseCase).execute(argThat((CreateKnowledgePointCommand command) ->
                command.cefrLevel() == CefrLevel.B1
                        && "Connect events to a reference time.".equals(command.canDo())
                        && "Present perfect".equals(command.scopeIn())
                        && "Perfect continuous forms".equals(command.scopeOut())));
    }

    @Test
    void rejectsMissingLearningTypeOnKnowledgePointCreation() throws Exception {
        mockMvc.perform(post("/api/content/knowledge-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"%s","code":"R55_EXAMPLE","name":"Example","kind":"GRAMMAR"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verify(createKnowledgePointUseCase, never()).execute(any());
    }

    @Test
    void rejectsNonCanonicalLearningType() throws Exception {
        mockMvc.perform(post("/api/content/knowledge-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"%s","code":"R55_EXAMPLE","name":"Example","kind":"GRAMMAR","learningType":"GRAMMAR"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verify(createKnowledgePointUseCase, never()).execute(any());
    }

    @Test
    void ignoresLegacyBandFieldsOnKnowledgePointCreation() throws Exception {
        when(createKnowledgePointUseCase.execute(any())).thenReturn(result(UUID.randomUUID(), LearningType.MEMORY));

        mockMvc.perform(post("/api/content/knowledge-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"%s","code":"R55_EXAMPLE","name":"Example","kind":"GRAMMAR","learningType":"MEMORY",
                                 "bandMin":6.0,"bandMax":5.0}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bandMin").doesNotExist());

        verify(createKnowledgePointUseCase).execute(any());
    }

    @Test
    void createsKnowledgePointWithInvalidCodeAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/content/knowledge-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"%s","code":"BAD-CODE","name":"Example","kind":"GRAMMAR","learningType":"CONCEPT"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verify(createKnowledgePointUseCase, never()).execute(any());
    }

    @Test
    void updatesKnowledgePointTopicAndCode() throws Exception {
        UUID id = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-17T00:00:00Z");
        when(updateKnowledgePointUseCase.execute(org.mockito.ArgumentMatchers.eq(id), any()))
                .thenReturn(new KnowledgePointResult(id, topicId, "R60_UPDATED", "Updated", KnowledgePointKind.GRAMMAR,
                        LearningType.CONCEPT, null, null, ContentStatus.ACTIVE, now, now, CefrLevel.B2,
                        "Can express a clear comparison.", "Basic comparatives", "Rhetorical comparisons"));

        mockMvc.perform(put("/api/content/knowledge-points/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"%s","code":"R60_UPDATED","name":"Updated","kind":"GRAMMAR","learningType":"CONCEPT",
                                 "cefrLevel":"B2","canDo":"Can express a clear comparison.",
                                 "scopeIn":"Basic comparatives","scopeOut":"Rhetorical comparisons"}
                                """.formatted(topicId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(topicId.toString()))
                .andExpect(jsonPath("$.code").value("R60_UPDATED"))
                .andExpect(jsonPath("$.cefrLevel").value("B2"))
                .andExpect(jsonPath("$.canDo").value("Can express a clear comparison."))
                .andExpect(jsonPath("$.scopeIn").value("Basic comparatives"))
                .andExpect(jsonPath("$.scopeOut").value("Rhetorical comparisons"));

        verify(updateKnowledgePointUseCase).execute(org.mockito.ArgumentMatchers.eq(id),
                argThat((UpdateKnowledgePointCommand command) -> command.cefrLevel() == CefrLevel.B2
                        && "Can express a clear comparison.".equals(command.canDo())
                        && "Basic comparatives".equals(command.scopeIn())
                        && "Rhetorical comparisons".equals(command.scopeOut())));
    }

    @Test
    void rejectsChangingTopicOfKnowledgePointInUse() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateKnowledgePointUseCase.execute(org.mockito.ArgumentMatchers.eq(id), any()))
                .thenThrow(new KnowledgePointInUseException(id));

        mockMvc.perform(put("/api/content/knowledge-points/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"%s","code":"R60_UPDATED","name":"Updated","kind":"GRAMMAR","learningType":"CONCEPT"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.code").value("KNOWLEDGE_POINT_IN_USE"));
    }

    @Test
    void deleteIsIdempotentAndReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(deactivateKnowledgePointUseCase).execute(id);

        mockMvc.perform(delete("/api/content/knowledge-points/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/content/knowledge-points/{id}", id)).andExpect(status().isNoContent());

        verify(deactivateKnowledgePointUseCase, times(2)).execute(id);
    }

    @Test
    void activateReturnsKnowledgePointAndRejectsMissingLearningType() throws Exception {
        UUID id = UUID.randomUUID();
        when(activateKnowledgePointUseCase.execute(id)).thenReturn(result(id, LearningType.CONCEPT));
        mockMvc.perform(post("/api/content/knowledge-points/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        when(activateKnowledgePointUseCase.execute(id)).thenThrow(
                new IllegalArgumentException("learningType is required for an active knowledge point"));
        mockMvc.perform(post("/api/content/knowledge-points/{id}/activate", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readsPrerequisitesAndMapsCycleInvalidIdsAndMissingKnowledgePoints() throws Exception {
        UUID id = UUID.randomUUID();
        UUID prerequisiteId = UUID.randomUUID();
        when(getKnowledgePointPrerequisitesUseCase.execute(id))
                .thenReturn(List.of(result(prerequisiteId, LearningType.CONCEPT)));
        mockMvc.perform(get("/api/content/knowledge-points/{id}/prerequisites", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(prerequisiteId.toString()));

        List<UUID> cycle = List.of(id, prerequisiteId, id);
        when(replaceKnowledgePointPrerequisitesUseCase.execute(id, List.of(prerequisiteId)))
                .thenThrow(new PrerequisiteCycleException(cycle));
        mockMvc.perform(put("/api/content/knowledge-points/{id}/prerequisites", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prerequisiteIds\":[\"%s\"]}".formatted(prerequisiteId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.code").value("PREREQUISITE_CYCLE"))
                .andExpect(jsonPath("$.details.cycle[0]").value(id.toString()))
                .andExpect(jsonPath("$.details.cycle[1]").value(prerequisiteId.toString()))
                .andExpect(jsonPath("$.details.cycle[2]").value(id.toString()));

        UUID invalidId = UUID.randomUUID();
        when(replaceKnowledgePointPrerequisitesUseCase.execute(id, List.of(invalidId)))
                .thenThrow(new InvalidPrerequisiteException(List.of(invalidId)));
        mockMvc.perform(put("/api/content/knowledge-points/{id}/prerequisites", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prerequisiteIds\":[\"%s\"]}".formatted(invalidId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.code").value("PREREQUISITE_INVALID"))
                .andExpect(jsonPath("$.details.invalidIds[0]").value(invalidId.toString()));

        when(getKnowledgePointPrerequisitesUseCase.execute(id)).thenThrow(new KnowledgePointNotFoundException(id));
        mockMvc.perform(get("/api/content/knowledge-points/{id}/prerequisites", id))
                .andExpect(status().isNotFound());
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
