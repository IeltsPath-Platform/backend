package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.ResolveContentReportCommand;
import com.ieltspath.community.application.command.SubmitContentReportCommand;
import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.aggregate.ContentReport;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.exception.CommunityConflictException;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.ContentReportRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.domain.vo.ReportContentType;
import com.ieltspath.community.domain.vo.ReportDecision;
import com.ieltspath.community.domain.vo.ReportStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentReportResolutionTest {
    private final ContentReportRepository reports = mock(ContentReportRepository.class);
    private final PostRepository posts = mock(PostRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);

    @Test
    void userCanReportAnActiveCommentOfAnotherUser() {
        UUID reporter = UUID.randomUUID();
        Comment comment = Comment.create(UUID.randomUUID(), UUID.randomUUID(), null, "rude");
        when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(reports.save(any(ContentReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = new SubmitContentReportUseCase(reports, posts, comments).execute(new CommunityActor(reporter, false),
                new SubmitContentReportCommand(ReportContentType.COMMENT, comment.getId(), "Abuse"));

        assertEquals(ReportStatus.PENDING, result.status());
        assertEquals("Abuse", result.reason());
        verify(reports).save(any(ContentReport.class));
    }

    @Test
    void reportOnAHiddenDeletedOrMissingCommentIsNotFound() {
        UUID author = UUID.randomUUID();
        Comment hidden = Comment.create(UUID.randomUUID(), author, null, "hidden");
        hidden.moderate(ContentStatus.HIDDEN);
        Comment deleted = Comment.create(UUID.randomUUID(), author, null, "deleted");
        deleted.delete(author);
        UUID missing = UUID.randomUUID();
        when(comments.findById(hidden.getId())).thenReturn(Optional.of(hidden));
        when(comments.findById(deleted.getId())).thenReturn(Optional.of(deleted));
        when(comments.findById(missing)).thenReturn(Optional.empty());
        var useCase = new SubmitContentReportUseCase(reports, posts, comments);

        for (UUID id : List.of(hidden.getId(), deleted.getId(), missing)) {
            var error = assertThrows(CommunityNotFoundException.class, () -> useCase.execute(
                    new CommunityActor(UUID.randomUUID(), false), new SubmitContentReportCommand(ReportContentType.COMMENT, id, "Abuse")));
            assertEquals("Comment not found", error.getMessage());
        }
        verify(reports, never()).save(any());
    }

    @Test
    void hidingAReportedPostHidesThePostAndActionsTheReport() {
        UUID admin = UUID.randomUUID();
        Post post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, "title", "body");
        ContentReport report = ContentReport.create(UUID.randomUUID(), ReportContentType.POST, post.getId(), "Spam");
        when(reports.findById(report.getId())).thenReturn(Optional.of(report));
        when(reports.findPendingByContentForUpdate(ReportContentType.POST, post.getId())).thenReturn(List.of(report));
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        when(posts.save(post)).thenReturn(post);
        when(reports.actionPendingForContent(eq(ReportContentType.POST), eq(post.getId()), eq(admin), any())).thenReturn(1);

        var result = new ResolveContentReportUseCase(reports, posts, comments).execute(new CommunityActor(admin, true),
                new ResolveContentReportCommand(report.getId(), ReportDecision.HIDE_CONTENT));

        assertEquals(ContentStatus.HIDDEN, post.getStatus());
        assertEquals(ReportStatus.ACTIONED, result.status());
        verify(posts).save(post);
    }

    @Test
    void resolvingAReportThatDoesNotExistIsNotFoundForBothDecisions() {
        UUID reportId = UUID.randomUUID();
        when(reports.findById(reportId)).thenReturn(Optional.empty());
        when(reports.findByIdForUpdate(reportId)).thenReturn(Optional.empty());
        var useCase = new ResolveContentReportUseCase(reports, posts, comments);
        var admin = new CommunityActor(UUID.randomUUID(), true);

        for (ReportDecision decision : ReportDecision.values()) {
            var error = assertThrows(CommunityNotFoundException.class,
                    () -> useCase.execute(admin, new ResolveContentReportCommand(reportId, decision)));
            assertEquals("Report not found", error.getMessage());
        }
    }

    @Test
    void hidingContentForAReportAnotherAdminAlreadyResolvedIsAConflict() {
        UUID admin = UUID.randomUUID();
        Post post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, "title", "body");
        ContentReport report = ContentReport.create(UUID.randomUUID(), ReportContentType.POST, post.getId(), "Spam");
        when(reports.findById(report.getId())).thenReturn(Optional.of(report));
        when(reports.findPendingByContentForUpdate(ReportContentType.POST, post.getId())).thenReturn(List.of());

        var error = assertThrows(CommunityConflictException.class,
                () -> new ResolveContentReportUseCase(reports, posts, comments).execute(new CommunityActor(admin, true),
                        new ResolveContentReportCommand(report.getId(), ReportDecision.HIDE_CONTENT)));

        assertEquals("Report is no longer pending", error.getMessage());
        verify(posts, never()).save(any());
    }
}
