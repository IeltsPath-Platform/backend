package com.group01.community.api.controller;

import com.group01.community.api.dto.CommunityDtos.*;
import com.group01.community.application.CommunityService;
import com.group01.community.domain.aggregate.*;
import com.group01.community.domain.repository.PageResult;
import com.group01.community.domain.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService service;

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody PostRequest r) {
        return post(service.createPost(r.category(), r.title(), r.body()));
    }

    @GetMapping("/posts")
    public PageResponse<PostResponse> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResult<Post> result = service.listPosts(page, size);
        var ids = result.content().stream().map(Post::getId).collect(java.util.stream.Collectors.toSet());
        var counts = service.reactionCounts(ids);
        return new PageResponse<>(
                result.content().stream()
                        .map(p -> post(p, counts.getOrDefault(p.getId(), java.util.Map.of())))
                        .toList(),
                page,
                size,
                result.totalElements(),
                result.totalPages(),
                "createdAt,desc;id,desc"
        );
    }

    @GetMapping("/posts/{id}")
    public PostResponse get(@PathVariable("id") UUID id) {
        return post(service.getPost(id));
    }

    @PutMapping("/posts/{id}")
    public PostResponse edit(@PathVariable("id") UUID id, @Valid @RequestBody PostRequest r) {
        return post(service.editPost(id, r.category(), r.title(), r.body()));
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) {
        service.deletePost(id);
    }

    @PutMapping("/posts/{id}/status")
    public PostResponse moderate(@PathVariable("id") UUID id, @Valid @RequestBody ModerationRequest r) {
        return post(service.moderatePost(id, r.status()));
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(@PathVariable("postId") UUID postId, @Valid @RequestBody CommentRequest r) {
        return comment(service.createComment(postId, r.parentCommentId(), r.body()));
    }

    @GetMapping("/posts/{postId}/comments")
    public PageResponse<CommentResponse> comments(
            @PathVariable("postId") UUID postId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResult<Comment> result = service.listComments(postId, page, size);
        return new PageResponse<>(
                result.content().stream().map(this::comment).toList(),
                page,
                size,
                result.totalElements(),
                result.totalPages(),
                "createdAt,asc;id,asc"
        );
    }

    @PutMapping("/comments/{id}")
    public CommentResponse editComment(@PathVariable("id") UUID id, @Valid @RequestBody UpdateCommentRequest r) {
        return comment(service.editComment(id, r.body()));
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable("id") UUID id) {
        service.deleteComment(id);
    }

    @PutMapping("/comments/{id}/status")
    public CommentResponse moderateComment(@PathVariable("id") UUID id, @Valid @RequestBody ModerationRequest r) {
        return comment(service.moderateComment(id, r.status()));
    }

    @PutMapping("/posts/{postId}/reactions/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void react(@PathVariable("postId") UUID postId, @PathVariable("type") ReactionType type) {
        service.addReaction(postId, type);
    }

    @DeleteMapping("/posts/{postId}/reactions/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unreact(@PathVariable("postId") UUID postId, @PathVariable("type") ReactionType type) {
        service.removeReaction(postId, type);
    }

    private PostResponse post(Post p) {
        return post(p, service.reactionCounts(p.getId()));
    }

    private PostResponse post(Post p, java.util.Map<ReactionType, Long> counts) {
        return new PostResponse(
                p.getId(),
                p.getAuthorId(),
                p.getCategory(),
                p.getTitle(),
                p.getBody(),
                p.getStatus(),
                counts,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private CommentResponse comment(Comment c) {
        return new CommentResponse(
                c.getId(),
                c.getPostId(),
                c.getAuthorId(),
                c.getParentCommentId(),
                c.getBody(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
