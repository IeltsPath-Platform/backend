package com.ieltspath.community.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonsecurity.role.CanonicalRoles;
import com.ieltspath.community.api.dto.request.CommentRequest;
import com.ieltspath.community.api.dto.request.CreateContentReportRequest;
import com.ieltspath.community.api.dto.request.ModerationRequest;
import com.ieltspath.community.api.dto.request.PostRequest;
import com.ieltspath.community.api.dto.request.ResolveContentReportRequest;
import com.ieltspath.community.api.dto.request.UpdateCommentRequest;
import com.ieltspath.community.api.dto.response.CommentResponse;
import com.ieltspath.community.api.dto.response.ContentReportQueueItemResponse;
import com.ieltspath.community.api.dto.response.ContentReportResponse;
import com.ieltspath.community.api.dto.response.PageResponse;
import com.ieltspath.community.api.dto.response.PostResponse;
import com.ieltspath.community.api.dto.response.CursorResponse;
import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.application.command.*;
import com.ieltspath.community.application.query.PageQuery;
import com.ieltspath.community.application.result.CommentResult;
import com.ieltspath.community.application.result.PageResult;
import com.ieltspath.community.application.result.PostResult;
import com.ieltspath.community.application.usecase.*;
import com.ieltspath.community.domain.vo.ReactionType;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.domain.vo.Relationship;
import com.ieltspath.community.domain.vo.FeedScope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {
    private final CurrentUserProvider currentUserProvider;
    private final CreatePostUseCase createPostUseCase;
    private final GetPostUseCase getPostUseCase;
    private final ListPostsUseCase listPostsUseCase;
    private final EditPostUseCase editPostUseCase;
    private final DeletePostUseCase deletePostUseCase;
    private final ModeratePostUseCase moderatePostUseCase;
    private final CreateCommentUseCase createCommentUseCase;
    private final ListCommentsUseCase listCommentsUseCase;
    private final EditCommentUseCase editCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final ModerateCommentUseCase moderateCommentUseCase;
    private final AddReactionUseCase addReactionUseCase;
    private final RemoveReactionUseCase removeReactionUseCase;
    private final CommunityQueriesUseCase queries;
    private final ManageSocialUseCase social;
    private final PublicProfileClient profiles;
    private final SubmitContentReportUseCase submitContentReportUseCase;
    private final ListPendingContentReportsUseCase listPendingContentReportsUseCase;
    private final ResolveContentReportUseCase resolveContentReportUseCase;

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentReportResponse report(@Valid @RequestBody CreateContentReportRequest request) {
        var result = submitContentReportUseCase.execute(actor(), new SubmitContentReportCommand(
                request.contentType(), request.contentId(), request.reason()));
        return ContentReportResponse.from(result);
    }

    @GetMapping("/admin/reports")
    public PageResponse<ContentReportQueueItemResponse> pendingReports(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        var result = listPendingContentReportsUseCase.execute(administrator(), new PageQuery(page, size));
        return new PageResponse<>(result.content().stream().map(ContentReportQueueItemResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(), "createdAt,asc;id,asc");
    }

    @PutMapping("/admin/reports/{reportId}/decision")
    public ContentReportResponse resolveReport(@PathVariable("reportId") UUID reportId,
                                                @Valid @RequestBody ResolveContentReportRequest request) {
        var result = resolveContentReportUseCase.execute(administrator(), new ResolveContentReportCommand(reportId, request.decision()));
        return ContentReportResponse.from(result);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody PostRequest request) {
        var currentActor = actor();
        PostResult result = createPostUseCase.execute(currentActor, new CreatePostCommand(
                request.category(), request.title(), request.body(), request.imageUrls(), request.videoUrl(), request.targetBand(), request.background()
        ));
        return PostResponse.from(queries.enrichPosts(currentActor, java.util.List.of(result)).getFirst());
    }

    @GetMapping("/posts")
    public ResponseEntity<?> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "pagination", required = false) String pagination,
            @RequestParam(name = "category", required = false) PostCategory category,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "scope", defaultValue = "ALL") FeedScope scope,
            @RequestParam(name = "q", required = false) String searchQuery
    ) {
        int pageSize = size == null ? ("cursor".equals(pagination) ? 10 : 20) : size;
        if ("cursor".equals(pagination)) {
            var currentActor = actor();
            var result = searchQuery == null || searchQuery.isBlank()
                    ? queries.feed(currentActor, null, false, scope == FeedScope.FOLLOWING, category, cursor, pageSize)
                    : queries.searchFeed(currentActor, searchQuery, scope == FeedScope.FOLLOWING, category, cursor, pageSize);
            return ResponseEntity.ok(new CursorResponse<>(result.content().stream().map(PostResponse::from).toList(),
                    result.nextCursor(), result.hasNext(), result.size()));
        }
        if (searchQuery != null && !searchQuery.isBlank()) {
            throw new com.ieltspath.community.domain.exception.CommunityException("Search requires cursor pagination");
        }
        if (scope == FeedScope.FOLLOWING) throw new com.ieltspath.community.domain.exception.CommunityException("Following feed requires cursor pagination");
        var query = new PageQuery(page, pageSize);
        PageResult<PostResult> result = category == null
                ? listPostsUseCase.execute(query)
                : listPostsUseCase.execute(query, category);
        var enrichedPosts = queries.enrichPosts(actor(), result.content());
        return ResponseEntity.ok(new PageResponse<>(
                enrichedPosts.stream().map(PostResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(),
                "createdAt,desc;id,desc"
        ));
    }

    @GetMapping("/posts/{id:[a-fA-F0-9\\-]{36}}")
    public PostResponse get(@PathVariable("id") UUID id) {
        var currentActor = actor();
        var result = getPostUseCase.execute(id, currentActor);
        return PostResponse.from(queries.enrichPosts(currentActor, java.util.List.of(result)).getFirst());
    }

    @PutMapping("/posts/{id:[a-fA-F0-9\\-]{36}}")
    public PostResponse edit(@PathVariable("id") UUID id, @Valid @RequestBody PostRequest request) {
        var currentActor = actor();
        PostResult result = editPostUseCase.execute(currentActor, new EditPostCommand(
                id, request.category(), request.title(), request.body(), request.imageUrls(), request.videoUrl(), request.targetBand(), request.background()
        ));
        return PostResponse.from(queries.enrichPosts(currentActor, java.util.List.of(result)).getFirst());
    }

    @DeleteMapping("/posts/{id:[a-fA-F0-9\\-]{36}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) {
        deletePostUseCase.execute(actor(), new DeletePostCommand(id));
    }

    @PutMapping("/posts/{id:[a-fA-F0-9\\-]{36}}/status")
    public PostResponse moderate(@PathVariable("id") UUID id, @Valid @RequestBody ModerationRequest request) {
        return PostResponse.from(moderatePostUseCase.execute(actor(), new ModeratePostCommand(id, request.status())));
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(
            @PathVariable("postId") UUID postId,
            @Valid @RequestBody CommentRequest request
    ) {
        var currentActor = actor();
        CommentResult result = createCommentUseCase.execute(currentActor, new CreateCommentCommand(
                postId, request.parentCommentId(), request.body()
        ));
        return CommentResponse.from(queries.enrichComments(java.util.List.of(result)).getFirst());
    }

    @GetMapping("/users/mention-suggestions")
    public java.util.List<com.ieltspath.community.application.result.PublicProfile> mentionSuggestions(
            @RequestParam("q") String query) {
        var actor = actor();
        var normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2 || normalized.length() > 80)
            throw new com.ieltspath.community.domain.exception.CommunityException("Mention search must contain 2 to 80 characters");
        return profiles.searchProfiles(actor.userId(), normalized, 8).stream()
                .filter(profile -> !profile.id().equals(actor.userId())).toList();
    }

    @GetMapping("/posts/{postId}/comments")
    public PageResponse<CommentResponse> comments(
            @PathVariable("postId") UUID postId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "threaded", defaultValue = "false") boolean threaded
    ) {
        if (threaded) {
            var result = queries.thread(actor(), postId, null, new PageQuery(page, size));
            return new PageResponse<>(result.content().stream().map(CommentResponse::from).toList(),
                    result.page(), result.size(), result.totalElements(), result.totalPages(), "createdAt,asc;id,asc");
        }
        PageResult<CommentResult> result = listCommentsUseCase.execute(actor(), postId, new PageQuery(page, size));
        var enrichedComments = queries.enrichComments(result.content());
        return new PageResponse<>(
                enrichedComments.stream().map(CommentResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(),
                "createdAt,asc;id,asc"
        );
    }

    @GetMapping("/comments/{commentId}/replies")
    public PageResponse<CommentResponse> replies(@PathVariable("commentId") UUID commentId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var result = queries.thread(actor(), null, commentId, new PageQuery(page, size));
        return new PageResponse<>(result.content().stream().map(CommentResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(), "createdAt,asc;id,asc");
    }

    @GetMapping("/users/{userId}/posts")
    public CursorResponse<PostResponse> userPosts(@PathVariable("userId") UUID userId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var currentActor = actor();
        queries.requireVisibleUser(currentActor, userId);
        var result = queries.feed(currentActor, userId, false, false, null, cursor, size);
        return new CursorResponse<>(result.content().stream().map(PostResponse::from).toList(),
                result.nextCursor(), result.hasNext(), result.size());
    }

    @GetMapping("/posts/bookmarked")
    public CursorResponse<PostResponse> bookmarked(@RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var result = queries.feed(actor(), null, true, false, null, cursor, size);
        return new CursorResponse<>(result.content().stream().map(PostResponse::from).toList(),
                result.nextCursor(), result.hasNext(), result.size());
    }

    @PostMapping("/posts/{postId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bookmark(@PathVariable("postId") UUID postId) { social.bookmark(actor(), postId); }

    @DeleteMapping("/posts/{postId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unbookmark(@PathVariable("postId") UUID postId) { social.unbookmark(actor(), postId); }

    @PostMapping("/users/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable("userId") UUID userId) { social.follow(actor(), userId); }

    @DeleteMapping("/users/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable("userId") UUID userId) { social.unfollow(actor(), userId); }

    @GetMapping("/users/{userId}/followers")
    public CursorResponse<com.ieltspath.community.application.result.PublicProfile> followers(
            @PathVariable("userId") UUID userId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var currentActor = actor();
        queries.requireVisibleUser(currentActor, userId);
        var result = queries.follows(userId, true, cursor, size);
        return new CursorResponse<>(result.content(), result.nextCursor(), result.hasNext(), result.size());
    }

    @GetMapping("/users/{userId}/following")
    public CursorResponse<com.ieltspath.community.application.result.PublicProfile> following(
            @PathVariable("userId") UUID userId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var currentActor = actor();
        queries.requireVisibleUser(currentActor, userId);
        var result = queries.follows(userId, false, cursor, size);
        return new CursorResponse<>(result.content(), result.nextCursor(), result.hasNext(), result.size());
    }

    @GetMapping("/users/{userId}/relationship")
    public Relationship relationship(@PathVariable("userId") UUID userId) {
        var currentActor = actor();
        queries.requireVisibleUser(currentActor, userId);
        return queries.relationship(currentActor, userId);
    }

    @PutMapping("/comments/{id}")
    public CommentResponse editComment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        var result = editCommentUseCase.execute(actor(), new EditCommentCommand(id, request.body()));
        return CommentResponse.from(queries.enrichComments(java.util.List.of(result)).getFirst());
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable("id") UUID id) {
        deleteCommentUseCase.execute(actor(), new DeleteCommentCommand(id));
    }

    @PutMapping("/comments/{id}/status")
    public CommentResponse moderateComment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ModerationRequest request
    ) {
        return CommentResponse.from(moderateCommentUseCase.execute(
                actor(), new ModerateCommentCommand(id, request.status())
        ));
    }

    @PutMapping("/posts/{postId}/reactions/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void react(@PathVariable("postId") UUID postId, @PathVariable("type") ReactionType type) {
        addReactionUseCase.execute(actor(), new AddReactionCommand(postId, type));
    }

    @DeleteMapping("/posts/{postId}/reactions/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unreact(@PathVariable("postId") UUID postId, @PathVariable("type") ReactionType type) {
        removeReactionUseCase.execute(actor(), new RemoveReactionCommand(postId, type));
    }

    private CommunityActor actor() {
        var user = currentUserProvider.requireCurrentUser();
        return new CommunityActor(user.id(), user.hasRole(CanonicalRoles.ADMIN));
    }

    private CommunityActor administrator() {
        var currentActor = actor();
        if (!currentActor.administrator()) {
            throw new com.ieltspath.community.domain.exception.CommunityForbiddenException("Administrator role is required");
        }
        return currentActor;
    }
}
