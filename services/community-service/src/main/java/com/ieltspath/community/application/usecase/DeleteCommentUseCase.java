package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.DeleteCommentCommand;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCommentUseCase {
    private final CommentRepository comments;

    @Transactional
    public void execute(CommunityActor actor, DeleteCommentCommand command) {
        var comment = comments.findById(command.commentId())
                .orElseThrow(() -> new CommunityNotFoundException("Comment not found"));
        comment.delete(actor.userId());
        comments.save(comment);
    }
}
