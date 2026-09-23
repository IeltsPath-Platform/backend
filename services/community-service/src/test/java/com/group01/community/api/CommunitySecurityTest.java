package com.group01.community.api;

import com.group01.community.api.controller.CommunityController;
import com.group01.community.application.CommunityService;
import com.group01.community.domain.aggregate.Comment;
import com.group01.community.domain.repository.PageResult;
import com.group01.community.domain.vo.ContentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommunityController.class)
@TestPropertySource(properties={
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "app.auth.internal-jwt-secret=SGs1cjhXY1lBOEo4am8ycnVJQkNFWFJjSGhVZlNEVXFQMTZIMjlQMXN4bz0="
})
class CommunitySecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean CommunityService service;
    @Test void rawIdentityHeadersDoNotAuthenticate() throws Exception {
        mvc.perform(get("/api/community/posts").header("X-User-Id","00000000-0000-0000-0000-000000000001").header("X-User-Roles","ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test void explicitPostIdPathVariableBindsWithoutCompilerParameterMetadata() throws Exception {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant now = Instant.now();
        when(service.createComment(eq(postId), isNull(), eq("A comment")))
                .thenReturn(new Comment(UUID.randomUUID(), postId, authorId, null, "A comment",
                        ContentStatus.ACTIVE, now, now, 0L));

        mvc.perform(post("/api/community/posts/{postId}/comments", postId)
                        .with(jwt().jwt(token -> token.subject(authorId.toString()).claim("roles", List.of("CUSTOMER"))))
                        .contentType("application/json")
                        .content("{\"body\":\"A comment\"}"))
                .andExpect(status().isCreated());
    }

    @Test void explicitPaginationParametersBindWithoutCompilerParameterMetadata() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.listPosts(2, 5)).thenReturn(new PageResult<>(List.of(), 2, 5, 0, 0));
        when(service.reactionCounts(anySet())).thenReturn(java.util.Map.of());

        mvc.perform(get("/api/community/posts?page=2&size=5")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isOk());
    }
}
