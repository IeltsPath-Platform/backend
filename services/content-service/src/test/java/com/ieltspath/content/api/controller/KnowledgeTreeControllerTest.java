package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.result.KnowledgeTreeNodeResult;
import com.ieltspath.content.application.result.KnowledgeTreeResult;
import com.ieltspath.content.application.usecase.GetKnowledgeTreeUseCase;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = KnowledgeTreeController.class, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class KnowledgeTreeControllerTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean GetKnowledgeTreeUseCase getKnowledgeTree;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadKnowledgeTree() throws Exception {
        UUID nodeId = UUID.randomUUID();
        when(getKnowledgeTree.execute(null, null, false)).thenReturn(tree(nodeId, ContentStatus.ACTIVE));

        mockMvc.perform(get("/api/content/knowledge-tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].id").value(nodeId.toString()))
                .andExpect(jsonPath("$.nodes[0].cefrLevel").value("B1"))
                .andExpect(jsonPath("$.nodes[0].canDo").value("Identify the main idea."))
                .andExpect(jsonPath("$.nodes[0].scopeIn").value("Short texts"))
                .andExpect(jsonPath("$.nodes[0].scopeOut").value("Long-form inference"));
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void contentAuthorCanReadKnowledgeTree() throws Exception {
        when(getKnowledgeTree.execute(null, null, false)).thenReturn(tree(UUID.randomUUID(), ContentStatus.ACTIVE));

        mockMvc.perform(get("/api/content/knowledge-tree")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void examinerCanReadKnowledgeTree() throws Exception {
        when(getKnowledgeTree.execute(null, null, false)).thenReturn(tree(UUID.randomUUID(), ContentStatus.ACTIVE));

        mockMvc.perform(get("/api/content/knowledge-tree")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotReadKnowledgeTree() throws Exception {
        mockMvc.perform(get("/api/content/knowledge-tree")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void examinerCannotSeeInactiveNodesEvenWhenRequestingThem() throws Exception {
        UUID activeId = UUID.randomUUID();
        when(getKnowledgeTree.execute(null, null, false)).thenReturn(tree(activeId, ContentStatus.ACTIVE));
        when(getKnowledgeTree.execute(null, null, true)).thenReturn(tree(UUID.randomUUID(), ContentStatus.INACTIVE));

        mockMvc.perform(get("/api/content/knowledge-tree").param("includeInactive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(1))
                .andExpect(jsonPath("$.nodes[0].id").value(activeId.toString()))
                .andExpect(jsonPath("$.nodes[0].status").value("ACTIVE"));

        verify(getKnowledgeTree).execute(null, null, false);
    }

    private KnowledgeTreeResult tree(UUID id, ContentStatus status) {
        return new KnowledgeTreeResult(List.of(new KnowledgeTreeNodeResult(id, "R55_TREE", "Tree node",
                Skill.READING, KnowledgePointKind.GRAMMAR, LearningType.CONCEPT, new BigDecimal("5.5"),
                UUID.randomUUID(), UUID.randomUUID(), List.of(), status, CefrLevel.B1,
                "Identify the main idea.", "Short texts", "Long-form inference")), List.of());
    }
}
