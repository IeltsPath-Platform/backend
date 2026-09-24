package com.group01.community.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.commonsecurity.role.CanonicalRoles;
import com.group01.community.api.dto.request.CommentRequest;
import com.group01.community.api.dto.request.ModerationRequest;
import com.group01.community.api.dto.request.PostRequest;
import com.group01.community.api.dto.request.UpdateCommentRequest;
import com.group01.community.api.dto.response.CommentResponse;
import com.group01.community.api.dto.response.PageResponse;
import com.group01.community.api.dto.response.PostResponse;
import com.group01.community.application.CommunityActor;
import com.group01.community.application.command.*;
import com.group01.community.application.query.PageQuery;
import com.group01.community.application.result.CommentResult;
import com.group01.community.application.result.PageResult;
import com.group01.community.application.result.PostResult;
import com.group01.community.application.usecase.*;
import com.group01.community.domain.vo.ReactionType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody PostRequest request) {
        PostResult result = createPostUseCase.execute(actor(), new CreatePostCommand(
                request.category(), request.title(), request.body()
        ));
        return PostResponse.from(result);
    }

    @GetMapping("/posts")
    public PageResponse<PostResponse> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResult<PostResult> result = listPostsUseCase.execute(new PageQuery(page, size));
        return new PageResponse<>(
                result.content().stream().map(PostResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(),
                "createdAt,desc;id,desc"
        );
    }

    @GetMapping("/posts/{id}")
    public PostResponse get(@PathVariable("id") UUID id) {
        return PostResponse.from(getPostUseCase.execute(id, actor()));
    }

    @PutMapping("/posts/{id}")
    public PostResponse edit(@PathVariable("id") UUID id, @Valid @RequestBody PostRequest request) {
        PostResult result = editPostUseCase.execute(actor(), new EditPostCommand(
                id, request.category(), request.title(), request.body()
        ));
        return PostResponse.from(result);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) {
        deletePostUseCase.execute(actor(), new DeletePostCommand(id));
    }

    @PutMapping("/posts/{id}/status")
    public PostResponse moderate(@PathVariable("id") UUID id, @Valid @RequestBody ModerationRequest request) {
        return PostResponse.from(moderatePostUseCase.execute(actor(), new ModeratePostCommand(id, request.status())));
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(
            @PathVariable("postId") UUID postId,
            @Valid @RequestBody CommentRequest request
    ) {
        CommentResult result = createCommentUseCase.execute(actor(), new CreateCommentCommand(
                postId, request.parentCommentId(), request.body()
        ));
        return CommentResponse.from(result);
    }

    @GetMapping("/posts/{postId}/comments")
    public PageResponse<CommentResponse> comments(
            @PathVariable("postId") UUID postId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResult<CommentResult> result = listCommentsUseCase.execute(actor(), postId, new PageQuery(page, size));
        return new PageResponse<>(
                result.content().stream().map(CommentResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(),
                "createdAt,asc;id,asc"
        );
    }

    @PutMapping("/comments/{id}")
    public CommentResponse editComment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return CommentResponse.from(editCommentUseCase.execute(actor(), new EditCommentCommand(id, request.body())));
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
}
