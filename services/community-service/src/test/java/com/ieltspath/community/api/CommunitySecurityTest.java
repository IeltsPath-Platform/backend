package com.ieltspath.community.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUser;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonsecurity.role.CanonicalRoles;
import com.ieltspath.community.api.controller.CommunityController;
import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.CreateCommentCommand;
import com.ieltspath.community.application.command.ModeratePostCommand;
import com.ieltspath.community.application.query.PageQuery;
import com.ieltspath.community.application.result.CommentResult;
import com.ieltspath.community.application.result.PostResult;
import com.ieltspath.community.application.result.CursorResult;
import com.ieltspath.community.application.result.ContentReportResult;
import com.ieltspath.community.application.result.PageResult;
import com.ieltspath.community.application.usecase.SubmitContentReportUseCase;
import com.ieltspath.community.application.usecase.ListPendingContentReportsUseCase;
import com.ieltspath.community.application.usecase.ResolveContentReportUseCase;
import com.ieltspath.community.domain.vo.ContentReportQueueEntry;
import com.ieltspath.community.application.result.ContentReportQueueItemResult;
import com.ieltspath.community.domain.vo.ReportContentType;
import com.ieltspath.community.domain.vo.ReportStatus;
import com.ieltspath.community.application.usecase.*;
import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @MockitoBean
    CommunityQueriesUseCase queries;
    @MockitoBean
    ManageSocialUseCase social;
    @MockitoBean
    PublicProfileClient profiles;
    @MockitoBean SubmitContentReportUseCase submitContentReportUseCase;
    @MockitoBean ListPendingContentReportsUseCase listPendingContentReportsUseCase;
    @MockitoBean ResolveContentReportUseCase resolveContentReportUseCase;

    @Test
    void reportEndpointsRequireAuthentication() throws Exception {
        mvc.perform(post("/api/community/reports").with(csrf()).contentType("application/json")
                        .content("{\"contentType\":\"POST\",\"contentId\":\"00000000-0000-0000-0000-000000000001\",\"reason\":\"Spam\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/community/admin/reports")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidReportPayloadReturnsBadRequest() throws Exception {
        UUID userId=UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(userId,Set.of(CanonicalRoles.CUSTOMER)));
        mvc.perform(post("/api/community/reports")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles",List.of("CUSTOMER"))))
                        .contentType("application/json")
                        .content("{\"contentType\":\"POST\",\"contentId\":\"00000000-0000-0000-0000-000000000001\",\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsReportWithCreatedStatus() throws Exception {
        UUID userId=UUID.randomUUID(), contentId=UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(userId,Set.of(CanonicalRoles.CUSTOMER)));
        when(submitContentReportUseCase.execute(eq(new CommunityActor(userId,false)),any()))
                .thenReturn(new ContentReportResult(UUID.randomUUID(),ReportContentType.POST,contentId,"Spam",ReportStatus.PENDING,Instant.now()));
        mvc.perform(post("/api/community/reports")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles",List.of("CUSTOMER"))))
                        .contentType("application/json")
                        .content("{\"contentType\":\"POST\",\"contentId\":\""+contentId+"\",\"reason\":\"Spam\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("POST"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void nonAdminCannotReadOrResolveReportQueue() throws Exception {
        UUID userId=UUID.randomUUID(), reportId=UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(userId,Set.of(CanonicalRoles.CUSTOMER)));
        var token=jwt().jwt(value->value.subject(userId.toString()).claim("roles",List.of("CUSTOMER")));
        mvc.perform(get("/api/community/admin/reports").with(token)).andExpect(status().isForbidden());
        mvc.perform(put("/api/community/admin/reports/{id}/decision",reportId).with(token)
                        .contentType("application/json").content("{\"decision\":\"DISMISS\"}"))
                .andExpect(status().isForbidden());
        verifyNoReportAdminUseCases();
    }

    @Test
    void adminCanReadQueueAndResolveReports() throws Exception {
        UUID adminId=UUID.randomUUID(), reportId=UUID.randomUUID(), targetId=UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(adminId,Set.of(CanonicalRoles.ADMIN)));
        var actor=new CommunityActor(adminId,true);
        when(listPendingContentReportsUseCase.execute(eq(actor),any(PageQuery.class))).thenReturn(
                new PageResult<>(List.of(new ContentReportQueueItemResult(reportId,UUID.randomUUID(),"Reporter",
                        ReportContentType.POST,targetId,UUID.randomUUID(),"Author","Reported post","Spam",
                        ReportStatus.PENDING,Instant.now())),0,20,1,1));
        when(resolveContentReportUseCase.execute(eq(actor),any())).thenReturn(new ContentReportResult(
                reportId,ReportContentType.POST,targetId,"Spam",ReportStatus.ACTIONED,Instant.now()));
        var token=jwt().jwt(value->value.subject(adminId.toString()).claim("roles",List.of("ADMIN")));
        mvc.perform(get("/api/community/admin/reports").with(token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].contentPreview").value("Reported post"));
        mvc.perform(put("/api/community/admin/reports/{id}/decision",reportId).with(token)
                        .contentType("application/json").content("{\"decision\":\"HIDE_CONTENT\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIONED"));
        verify(listPendingContentReportsUseCase).execute(eq(actor),any(PageQuery.class));
        verify(resolveContentReportUseCase).execute(eq(actor),any());
    }

    @Test
    void adminInvalidQueueAndDecisionInputsReturnBadRequest() throws Exception {
        UUID adminId=UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(adminId,Set.of(CanonicalRoles.ADMIN)));
        var token=jwt().jwt(value->value.subject(adminId.toString()).claim("roles",List.of("ADMIN")));
        mvc.perform(get("/api/community/admin/reports?size=0").with(token))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/community/admin/reports/{id}/decision",UUID.randomUUID()).with(token)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(listPendingContentReportsUseCase,resolveContentReportUseCase);
    }

    @Test
    void invalidAdminReportDecisionEnumReturnsBadRequest() throws Exception {
        UUID adminId=UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(adminId,Set.of(CanonicalRoles.ADMIN)));
        mvc.perform(put("/api/community/admin/reports/{id}/decision",UUID.randomUUID())
                        .with(jwt().jwt(value->value.subject(adminId.toString()).claim("roles",List.of("ADMIN"))))
                        .contentType("application/json").content("{\"decision\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(resolveContentReportUseCase);
    }

    @Test
    void invalidRequestBodyEnumsReturnBadRequest() throws Exception {
        UUID userId=UUID.randomUUID();
        var token=jwt().jwt(value->value.subject(userId.toString()).claim("roles",List.of("CUSTOMER")));
        mvc.perform(post("/api/community/reports").with(token).contentType("application/json")
                        .content("{\"contentType\":\"BOGUS\",\"contentId\":\"00000000-0000-0000-0000-000000000001\",\"reason\":\"Spam\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/community/posts").with(token).contentType("application/json")
                        .content("{\"category\":\"BOGUS\",\"body\":\"A valid post body\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/community/posts/{id}/status",UUID.randomUUID()).with(token).contentType("application/json")
                        .content("{\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest());
    }

    private void verifyNoReportAdminUseCases() {
        verifyNoInteractions(listPendingContentReportsUseCase,resolveContentReportUseCase);
    }

    @Test
    void rawIdentityHeadersDoNotAuthenticate() throws Exception {
        mvc.perform(get("/api/community/posts")
                        .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void commentValidationReturnsBadRequest() throws Exception {
        UUID userId=UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));
        mvc.perform(post("/api/community/posts/{postId}/comments", UUID.randomUUID())
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER"))))
                        .contentType("application/json").content("{\"body\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forbiddenCommentEditReturnsForbidden() throws Exception {
        UUID userId=UUID.randomUUID(), commentId=UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));
        when(editCommentUseCase.execute(eq(new CommunityActor(userId, false)), any()))
                .thenThrow(new com.ieltspath.community.domain.exception.CommunityForbiddenException("Only the author may edit this comment"));
        mvc.perform(put("/api/community/comments/{id}", commentId)
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER"))))
                        .contentType("application/json").content("{\"body\":\"edited\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCannotEditPostOfAnotherUser() throws Exception {
        UUID adminId = UUID.randomUUID(), postId = UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(adminId, Set.of(CanonicalRoles.ADMIN)));
        when(editPostUseCase.execute(eq(new CommunityActor(adminId, true)), any()))
                .thenThrow(new com.ieltspath.community.domain.exception.CommunityForbiddenException("Only the author may change this post"));
        mvc.perform(put("/api/community/posts/{id}", postId)
                        .with(jwt().jwt(token -> token.subject(adminId.toString()).claim("roles", List.of("ADMIN"))))
                        .contentType("application/json")
                        .content("{\"category\":\"QUESTION\",\"title\":\"New title\",\"body\":\"New body\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
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
        when(queries.enrichComments(any())).thenAnswer(invocation -> invocation.getArgument(0));

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
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(UUID.randomUUID(), Set.of(CanonicalRoles.CUSTOMER)));
        when(listPostsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 2, 5, 0, 0));
        when(queries.enrichPosts(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

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

    @Test
    void followingFeedRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/community/posts?pagination=cursor&scope=FOLLOWING"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void followingFeedRejectsUnsupportedScope() throws Exception {
        UUID userId = UUID.randomUUID();
        mvc.perform(get("/api/community/posts?pagination=cursor&scope=EVERYONE")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void followingFeedPassesCursorAndViewerScopeToQueryUseCase() throws Exception {
        UUID userId = UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));
        when(queries.feed(eq(new CommunityActor(userId, false)), eq(null), eq(false), eq(true),
                eq(PostCategory.QUESTION), eq("encoded-cursor"), eq(10)))
                .thenReturn(new CursorResult<>(List.of(), null, false, 10));

        mvc.perform(get("/api/community/posts?pagination=cursor&scope=FOLLOWING&category=QUESTION&cursor=encoded-cursor")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(queries).feed(eq(new CommunityActor(userId, false)), eq(null), eq(false), eq(true),
                eq(PostCategory.QUESTION), eq("encoded-cursor"), eq(10));
    }

    @Test
    void searchFeedPassesQueryScopeCategoryAndCursorToUseCase() throws Exception {
        UUID userId = UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));
        when(queries.searchFeed(any(CommunityActor.class), anyString(), anyBoolean(),
                any(PostCategory.class), anyString(), anyInt()))
                .thenReturn(new CursorResult<>(List.of(), null, false, 10));

        mvc.perform(get("/api/community/posts")
                        .param("pagination", "cursor")
                        .param("scope", "FOLLOWING")
                        .param("category", "QUESTION")
                        .param("q", "reading skills")
                        .param("cursor", "encoded-cursor")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(queries).searchFeed(eq(new CommunityActor(userId, false)), eq("reading skills"), eq(true),
                eq(PostCategory.QUESTION), eq("encoded-cursor"), eq(10));
    }

    @Test
    void searchRequiresCursorPagination() throws Exception {
        UUID userId = UUID.randomUUID();
        mvc.perform(get("/api/community/posts?q=reading")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonExistentPathReturnsNotFoundWithStandardErrorResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        mvc.perform(post("/api/community/posts/upload-image")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/community/posts/upload-image"));
    }

    @Test
    void malformedEnumQueryParameterReturnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        mvc.perform(get("/api/community/posts?category=INVALID_ENUM")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid value for category"));
    }

    @Test
    void malformedUuidPathVariableReturnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        mvc.perform(get("/api/community/posts/not-a-valid-uuid/comments")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid value for postId"));
    }

    @Test
    void missingMentionQueryParameterReturnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        mvc.perform(get("/api/community/users/mention-suggestions")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Required parameter is missing: q"));
    }

    @Test
    void unsupportedMediaTypeReturnsUnsupportedMediaType() throws Exception {
        UUID userId = UUID.randomUUID();
        mvc.perform(post("/api/community/posts")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER"))))
                        .contentType("text/plain")
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    @Test
    void unacceptableMediaTypeReturnsNotAcceptable() throws Exception {
        UUID userId = UUID.randomUUID();
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));
        when(listPostsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
        mvc.perform(get("/api/community/posts")
                        .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("CUSTOMER"))))
                        .accept("application/xml"))
                .andExpect(status().isNotAcceptable());
    }
}
