package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.AddCommentReactionCommand;
import com.ieltspath.community.domain.exception.CommunityForbiddenException;
import com.ieltspath.community.domain.repository.CommentReactionRepository;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddCommentReactionUseCase {
    private final CommentRepository comments;
    private final PostRepository posts;
    private final CommentReactionRepository reactions;
    private final UserBlockRepository blocks;

    @Autowired
    public AddCommentReactionUseCase(CommentRepository comments, PostRepository posts,
                                     CommentReactionRepository reactions, UserBlockRepository blocks) {
        this.comments = comments;
        this.posts = posts;
        this.reactions = reactions;
        this.blocks = blocks;
    }

    public AddCommentReactionUseCase(CommentRepository comments, PostRepository posts,
                                     CommentReactionRepository reactions) {
        this(comments, posts, reactions, CommunityApplicationSupport.noOpBlocks());
    }

    @Transactional
    public void execute(CommunityActor actor, AddCommentReactionCommand command) {
        var comment = CommunityApplicationSupport.requireVisibleComment(comments, posts, command.commentId(), actor);
        if (blocks.isBlockedEitherWay(actor.userId(), comment.getAuthorId())) {
            throw new CommunityForbiddenException("Không thể tương tác do có thiết lập chặn giữa hai người dùng", "USER_BLOCKED");
        }
        reactions.add(command.commentId(), actor.userId(), command.type());
    }
}
