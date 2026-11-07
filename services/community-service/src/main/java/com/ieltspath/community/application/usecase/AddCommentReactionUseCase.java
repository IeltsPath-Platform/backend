package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.AddCommentReactionCommand;
import com.ieltspath.community.domain.repository.CommentReactionRepository;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddCommentReactionUseCase {
    private final CommentRepository comments;
    private final PostRepository posts;
    private final CommentReactionRepository reactions;

    @Transactional
    public void execute(CommunityActor actor, AddCommentReactionCommand command) {
        CommunityApplicationSupport.requireVisibleComment(comments, posts, command.commentId(), actor);
        reactions.add(command.commentId(), actor.userId(), command.type());
    }
}
