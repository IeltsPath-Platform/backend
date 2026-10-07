package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.EditCommentCommand;
import com.ieltspath.community.application.result.CommentResult;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditCommentUseCase {
    private final CommentRepository comments;

    @Transactional
    public CommentResult execute(CommunityActor actor, EditCommentCommand command) {
        var comment = comments.findById(command.commentId())
                .orElseThrow(() -> new CommunityNotFoundException("Comment not found"));
        comment.edit(actor.userId(), command.body());
        return CommentResult.from(comments.save(comment));
    }
}
