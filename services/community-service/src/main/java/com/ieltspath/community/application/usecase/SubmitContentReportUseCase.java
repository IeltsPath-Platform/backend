package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.SubmitContentReportCommand;
import com.ieltspath.community.application.result.ContentReportResult;
import com.ieltspath.community.domain.aggregate.ContentReport;
import com.ieltspath.community.domain.exception.CommunityForbiddenException;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.ContentReportRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.ReportContentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitContentReportUseCase {
    private final ContentReportRepository reports;
    private final PostRepository posts;
    private final CommentRepository comments;

    @Transactional
    public ContentReportResult execute(CommunityActor actor, SubmitContentReportCommand command) {
        var authorId = switch (command.contentType()) {
            case POST -> posts.findById(command.contentId())
                    .filter(post -> post.getStatus() == ContentStatus.ACTIVE)
                    .orElseThrow(() -> new CommunityNotFoundException("Post not found")).getAuthorId();
            case COMMENT -> comments.findById(command.contentId())
                    .filter(comment -> comment.getStatus() == ContentStatus.ACTIVE)
                    .orElseThrow(() -> new CommunityNotFoundException("Comment not found")).getAuthorId();
        };
        if (authorId.equals(actor.userId())) throw new CommunityForbiddenException("You cannot report your own content");
        return ContentReportResult.from(reports.save(ContentReport.create(actor.userId(), command.contentType(),
                command.contentId(), command.reason())));
    }
}
