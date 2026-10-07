package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.ModerateCommentCommand;
import com.ieltspath.community.application.result.CommentResult;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModerateCommentUseCase {
    private final CommentRepository comments;

    @Transactional
    public CommentResult execute(CommunityActor actor, ModerateCommentCommand command) {
        CommunityApplicationSupport.requireAdministrator(actor);
        var comment = comments.findById(command.commentId())
                .orElseThrow(() -> new CommunityNotFoundException("Comment not found"));
        comment.moderate(command.status());
        return CommentResult.from(comments.save(comment));
    }
}
