package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.ResolveContentReportCommand;
import com.ieltspath.community.application.result.ContentReportResult;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.exception.CommunityConflictException;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.ContentReportRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.ReportContentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ResolveContentReportUseCase {
    private final ContentReportRepository reports;
    private final PostRepository posts;
    private final CommentRepository comments;

    @Transactional
    public ContentReportResult execute(CommunityActor actor, ResolveContentReportCommand command) {
        CommunityApplicationSupport.requireAdministrator(actor);
        if (command.decision() == com.ieltspath.community.domain.vo.ReportDecision.HIDE_CONTENT) {
            var snapshot = reports.findById(command.reportId())
                    .orElseThrow(() -> new CommunityNotFoundException("Report not found"));
            var pending = reports.findPendingByContentForUpdate(snapshot.getContentType(), snapshot.getContentId());
            var report = pending.stream().filter(item -> item.getId().equals(command.reportId())).findFirst()
                    .orElseThrow(() -> new CommunityConflictException("Report is no longer pending"));
            hideReportedContent(report);
            var reviewedAt = Instant.now();
            pending.forEach(item -> item.resolve(actor.userId(), command.decision()));
            int resolved = reports.actionPendingForContent(report.getContentType(), report.getContentId(), actor.userId(), reviewedAt);
            if (resolved != pending.size()) throw new CommunityConflictException("Pending reports changed during resolution");
            return ContentReportResult.from(report);
        }

        var report = reports.findByIdForUpdate(command.reportId())
                .orElseThrow(() -> new CommunityNotFoundException("Report not found"));
        report.resolve(actor.userId(), command.decision());
        return ContentReportResult.from(reports.update(report));
    }

    private void hideReportedContent(com.ieltspath.community.domain.aggregate.ContentReport report) {
        if (report.getContentType() == ReportContentType.POST) {
            var postOpt = posts.findById(report.getContentId());
            if (postOpt.isPresent()) {
                var post = postOpt.get();
                if (post.getStatus() != ContentStatus.DELETED) {
                    post.moderate(ContentStatus.HIDDEN);
                    posts.save(post);
                }
            }
            return;
        }
        var commentOpt = comments.findById(report.getContentId());
        if (commentOpt.isPresent()) {
            var comment = commentOpt.get();
            if (comment.getStatus() != ContentStatus.DELETED) {
                comment.moderate(ContentStatus.HIDDEN);
                comments.save(comment);
            }
        }
    }
}
