package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.application.command.ResolveContentReportCommand;
import com.ieltspath.community.application.command.SubmitContentReportCommand;
import com.ieltspath.community.application.result.PublicProfile;
import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.aggregate.ContentReport;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.exception.CommunityForbiddenException;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.ContentReportRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.ContentReportQueueEntry;
import com.ieltspath.community.domain.vo.CommunityPage;
import com.ieltspath.community.domain.vo.ReportContentType;
import com.ieltspath.community.domain.vo.ReportDecision;
import com.ieltspath.community.domain.vo.ReportStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentReportUseCaseTest {
    private final ContentReportRepository reports = mock(ContentReportRepository.class);
    private final PostRepository posts = mock(PostRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final PublicProfileClient profiles = mock(PublicProfileClient.class);

    @Test void submitsOneReportForActivePostOwnedByAnotherUser() {
        UUID author=UUID.randomUUID(), reporter=UUID.randomUUID();
        Post post=Post.create(author,com.ieltspath.community.domain.vo.PostCategory.GENERAL,"title","body");
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        when(reports.save(any())).thenAnswer(call -> call.getArgument(0));

        var result=new SubmitContentReportUseCase(reports,posts,comments).execute(new CommunityActor(reporter,false),
                new SubmitContentReportCommand(ReportContentType.POST,post.getId(),"  Spam content  "));

        assertEquals(ReportStatus.PENDING,result.status());
        assertEquals("Spam content",result.reason());
        verify(reports).save(argThat(report -> report.getReporterId().equals(reporter)
                && report.getContentType()==ReportContentType.POST && report.getContentId().equals(post.getId())));
    }

    @Test void rejectsSelfReportAndInactiveContentWithoutSaving() {
        UUID author=UUID.randomUUID();
        Post own=Post.create(author,com.ieltspath.community.domain.vo.PostCategory.GENERAL,"title","body");
        when(posts.findById(own.getId())).thenReturn(Optional.of(own));
        var submit=new SubmitContentReportUseCase(reports,posts,comments);
        assertThrows(CommunityForbiddenException.class,()->submit.execute(new CommunityActor(author,false),
                new SubmitContentReportCommand(ReportContentType.POST,own.getId(),"Reason")));

        Post hidden=Post.create(UUID.randomUUID(),com.ieltspath.community.domain.vo.PostCategory.GENERAL,"title","body");
        hidden.moderate(ContentStatus.HIDDEN);
        when(posts.findById(hidden.getId())).thenReturn(Optional.of(hidden));
        assertThrows(CommunityNotFoundException.class,()->submit.execute(new CommunityActor(UUID.randomUUID(),false),
                new SubmitContentReportCommand(ReportContentType.POST,hidden.getId(),"Reason")));
        verify(reports,never()).save(any());
    }

    @Test void rejectsBlankReasonBeforePersistence() {
        assertThrows(CommunityException.class,()->ContentReport.create(UUID.randomUUID(),ReportContentType.COMMENT,
                UUID.randomUUID(),"  "));
        verifyNoInteractions(reports,posts,comments);
    }

    @Test void onlyAdministratorCanReadPendingQueue() {
        var useCase=new ListPendingContentReportsUseCase(reports,profiles);
        assertThrows(CommunityForbiddenException.class,()->useCase.execute(new CommunityActor(UUID.randomUUID(),false),
                new com.ieltspath.community.application.query.PageQuery(0,20)));
        verifyNoInteractions(reports,profiles);
    }

    @Test void adminQueueResolvesReporterAndAuthorNamesWithOneBatch() {
        UUID reporter=UUID.randomUUID(), author=UUID.randomUUID(), content=UUID.randomUUID();
        var entry=new ContentReportQueueEntry(UUID.randomUUID(),reporter,ReportContentType.POST,content,author,
                "A post","Spam",ReportStatus.PENDING,java.time.Instant.now());
        when(reports.findPending(0,20)).thenReturn(new CommunityPage<>(java.util.List.of(entry),1));
        when(profiles.findProfiles(java.util.Set.of(reporter,author))).thenReturn(java.util.Map.of(
                reporter,new PublicProfile(reporter,"Reporter Name",null,null,null,null),
                author,new PublicProfile(author,"Author Name",null,null,null,null)));

        var result=new ListPendingContentReportsUseCase(reports,profiles).execute(new CommunityActor(UUID.randomUUID(),true),
                new com.ieltspath.community.application.query.PageQuery(0,20));

        assertEquals("Reporter Name",result.content().getFirst().reporterDisplayName());
        assertEquals("Author Name",result.content().getFirst().contentAuthorDisplayName());
        verify(profiles,times(1)).findProfiles(java.util.Set.of(reporter,author));
    }

    @Test void onlyAdministratorCanResolveReports() {
        var useCase=new ResolveContentReportUseCase(reports,posts,comments);
        assertThrows(CommunityForbiddenException.class,()->useCase.execute(new CommunityActor(UUID.randomUUID(),false),
                new ResolveContentReportCommand(UUID.randomUUID(),ReportDecision.DISMISS)));
        verifyNoInteractions(reports,posts,comments);
    }

    @Test void administratorCanHideReportedCommentAndResolveReportAtomically() {
        UUID admin=UUID.randomUUID();
        Comment comment=Comment.create(UUID.randomUUID(),UUID.randomUUID(),null,"reported comment");
        ContentReport report=ContentReport.create(UUID.randomUUID(),ReportContentType.COMMENT,comment.getId(),"Abuse");
        when(reports.findById(report.getId())).thenReturn(Optional.of(report));
        when(reports.findPendingByContentForUpdate(ReportContentType.COMMENT,comment.getId())).thenReturn(java.util.List.of(report));
        when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(comments.save(comment)).thenReturn(comment);
        when(reports.actionPendingForContent(eq(ReportContentType.COMMENT),eq(comment.getId()),eq(admin),any())).thenReturn(1);

        var result=new ResolveContentReportUseCase(reports,posts,comments).execute(new CommunityActor(admin,true),
                new ResolveContentReportCommand(report.getId(),ReportDecision.HIDE_CONTENT));

        assertEquals(ContentStatus.HIDDEN,comment.getStatus());
        assertEquals(ReportStatus.ACTIONED,result.status());
        verify(comments).save(comment);
        verify(reports).actionPendingForContent(eq(ReportContentType.COMMENT),eq(comment.getId()),eq(admin),any());
    }

    @Test void adminCanDismissWithoutChangingContentAndCannotResolveTwice() {
        UUID admin=UUID.randomUUID();
        ContentReport report=ContentReport.create(UUID.randomUUID(),ReportContentType.POST,UUID.randomUUID(),"Not a violation");
        when(reports.findByIdForUpdate(report.getId())).thenReturn(Optional.of(report));
        when(reports.update(report)).thenReturn(report);
        var useCase=new ResolveContentReportUseCase(reports,posts,comments);
        var result=useCase.execute(new CommunityActor(admin,true),new ResolveContentReportCommand(report.getId(),ReportDecision.DISMISS));
        assertEquals(ReportStatus.DISMISSED,result.status());
        verifyNoInteractions(posts,comments);
        assertThrows(CommunityException.class,()->report.resolve(admin,ReportDecision.DISMISS));
    }

    @Test void hidingContentActionsEveryPendingReportForThatContent() {
        UUID admin=UUID.randomUUID();
        Post post=Post.create(UUID.randomUUID(),com.ieltspath.community.domain.vo.PostCategory.GENERAL,"title","body");
        UUID target=post.getId();
        ContentReport selected=ContentReport.create(UUID.randomUUID(),ReportContentType.POST,target,"Spam");
        ContentReport other=ContentReport.create(UUID.randomUUID(),ReportContentType.POST,target,"Harassment");
        when(reports.findById(selected.getId())).thenReturn(Optional.of(selected));
        when(reports.findPendingByContentForUpdate(ReportContentType.POST,target)).thenReturn(java.util.List.of(selected,other));
        when(posts.findById(target)).thenReturn(Optional.of(post));
        when(posts.save(post)).thenReturn(post);
        when(reports.actionPendingForContent(eq(ReportContentType.POST),eq(target),eq(admin),any())).thenReturn(2);

        new ResolveContentReportUseCase(reports,posts,comments).execute(new CommunityActor(admin,true),
                new ResolveContentReportCommand(selected.getId(),ReportDecision.HIDE_CONTENT));

        assertEquals(ReportStatus.ACTIONED,selected.getStatus());
        assertEquals(ReportStatus.ACTIONED,other.getStatus());
        verify(reports).actionPendingForContent(eq(ReportContentType.POST),eq(target),eq(admin),any());
    }

    @Test
    void hidingAlreadyDeletedOrMissingContentResolvesReportToActionedWithout404() {
        UUID admin = UUID.randomUUID();
        UUID missingPostId = UUID.randomUUID();
        ContentReport report = ContentReport.create(UUID.randomUUID(), ReportContentType.POST, missingPostId, "Offensive");

        when(reports.findById(report.getId())).thenReturn(Optional.of(report));
        when(reports.findPendingByContentForUpdate(ReportContentType.POST, missingPostId)).thenReturn(java.util.List.of(report));
        when(posts.findById(missingPostId)).thenReturn(Optional.empty());
        when(reports.actionPendingForContent(eq(ReportContentType.POST), eq(missingPostId), eq(admin), any())).thenReturn(1);

        var result = new ResolveContentReportUseCase(reports, posts, comments).execute(
                new CommunityActor(admin, true),
                new ResolveContentReportCommand(report.getId(), ReportDecision.HIDE_CONTENT)
        );

        assertEquals(ReportStatus.ACTIONED, result.status());
        verify(posts, never()).save(any());
        verify(reports).actionPendingForContent(eq(ReportContentType.POST), eq(missingPostId), eq(admin), any());
    }

    @Test
    void hidingDeletedCommentResolvesReportToActionedWithout404() {
        UUID admin = UUID.randomUUID();
        Comment deletedComment = Comment.create(UUID.randomUUID(), UUID.randomUUID(), null, "Spam");
        deletedComment.delete(deletedComment.getAuthorId());
        ContentReport report = ContentReport.create(UUID.randomUUID(), ReportContentType.COMMENT, deletedComment.getId(), "Spam");

        when(reports.findById(report.getId())).thenReturn(Optional.of(report));
        when(reports.findPendingByContentForUpdate(ReportContentType.COMMENT, deletedComment.getId())).thenReturn(java.util.List.of(report));
        when(comments.findById(deletedComment.getId())).thenReturn(Optional.of(deletedComment));
        when(reports.actionPendingForContent(eq(ReportContentType.COMMENT), eq(deletedComment.getId()), eq(admin), any())).thenReturn(1);

        var result = new ResolveContentReportUseCase(reports, posts, comments).execute(
                new CommunityActor(admin, true),
                new ResolveContentReportCommand(report.getId(), ReportDecision.HIDE_CONTENT)
        );

        assertEquals(ReportStatus.ACTIONED, result.status());
        verify(comments, never()).save(any());
        verify(reports).actionPendingForContent(eq(ReportContentType.COMMENT), eq(deletedComment.getId()), eq(admin), any());
    }
}
