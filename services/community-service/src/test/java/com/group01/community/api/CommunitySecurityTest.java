package com.group01.community.api;

import com.group01.commonsecurity.currentuser.CurrentUser;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.commonsecurity.role.CanonicalRoles;
import com.group01.community.api.controller.CommunityController;
import com.group01.community.application.CommunityActor;
import com.group01.community.application.command.CreateCommentCommand;
import com.group01.community.application.command.ModeratePostCommand;
import com.group01.community.application.query.PageQuery;
import com.group01.community.application.result.CommentResult;
import com.group01.community.application.result.PageResult;
import com.group01.community.application.result.PostResult;
import com.group01.community.application.usecase.*;
import com.group01.community.domain.vo.ContentStatus;
import com.group01.community.domain.vo.PostCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommunityController.class)
@TestPropertySource(properties = {
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "app.auth.internal-jwt-secret=SGs1cjhXY1lBOEo4am8ycnVJQkNFWFJjSGhVZlNEVXFQMTZIMjlQMXN4bz0="
})
class CommunitySecurityTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    CurrentUserProvider currentUserProvider;
    @MockitoBean
    CreatePostUseCase createPostUseCase;
    @MockitoBean
    GetPostUseCase getPostUseCase;
    @MockitoBean
    ListPostsUseCase listPostsUseCase;
    @MockitoBean
    EditPostUseCase editPostUseCase;
    @MockitoBean
    DeletePostUseCase deletePostUseCase;
    @MockitoBean
    ModeratePostUseCase moderatePostUseCase;
    @MockitoBean
    CreateCommentUseCase createCommentUseCase;
    @MockitoBean
    ListCommentsUseCase listCommentsUseCase;
    @MockitoBean
    EditCommentUseCase editCommentUseCase;
    @MockitoBean
    DeleteCommentUseCase deleteCommentUseCase;
    @MockitoBean
    ModerateCommentUseCase moderateCommentUseCase;
    @MockitoBean
    AddReactionUseCase addReactionUseCase;
    @MockitoBean
    RemoveReactionUseCase removeReactionUseCase;

    @Test
    void rawIdentityHeadersDoNotAuthenticate() throws Exception {
        mvc.perform(get("/api/community/posts")
                        .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mapsVerifiedJwtPrincipalToUseCaseActor() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        var result = new CommentResult(
                UUID.randomUUID(), UUID.randomUUID(), userId, null, "A comment", ContentStatus.ACTIVE, now, now
        );
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));
        when(createCommentUseCase.execute(eq(new CommunityActor(userId, false)), any(CreateCommentCommand.class)))
                .thenReturn(result);

        mvc.perform(post("/api/community/posts/{postId}/comments", result.postId())
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER"))))
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Roles", "ADMIN")
                        .contentType("application/json")
                        .content("{\"body\":\"A comment\"}"))
                .andExpect(status().isCreated());

        verify(createCommentUseCase).execute(
                eq(new CommunityActor(userId, false)), any(CreateCommentCommand.class)
        );
    }

    @Test
    void mapsVerifiedAdminRoleToAdministratorCapability() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Instant now = Instant.now();
        var result = new PostResult(
                postId, UUID.randomUUID(), PostCategory.GENERAL, "Title", "Body", ContentStatus.HIDDEN,
                java.util.Map.of(), now, now
        );
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(adminId, Set.of(CanonicalRoles.ADMIN)));
        when(moderatePostUseCase.execute(eq(new CommunityActor(adminId, true)), any(ModeratePostCommand.class)))
                .thenReturn(result);

        mvc.perform(put("/api/community/posts/{id}/status", postId)
                        .with(jwt().jwt(token -> token.subject(adminId.toString()).claim("roles", List.of("ADMIN"))))
                        .contentType("application/json")
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isOk());

        verify(moderatePostUseCase).execute(
                eq(new CommunityActor(adminId, true)), any(ModeratePostCommand.class)
        );
    }

    @Test
    void paginationParametersReachListUseCase() throws Exception {
        when(listPostsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 2, 5, 0, 0));

        mvc.perform(get("/api/community/posts?page=2&size=5")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                                .claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.sort").value("createdAt,desc;id,desc"));

        verify(listPostsUseCase).execute(eq(new PageQuery(2, 5)));
    }
}
