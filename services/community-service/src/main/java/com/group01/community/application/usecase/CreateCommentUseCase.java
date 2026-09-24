package com.group01.community.application.usecase;

import com.group01.community.application.CommunityActor;
import com.group01.community.application.command.CreateCommentCommand;
import com.group01.community.application.result.CommentResult;
import com.group01.community.domain.aggregate.Comment;
import com.group01.community.domain.exception.CommunityException;
import com.group01.community.domain.exception.CommunityNotFoundException;
import com.group01.community.domain.repository.CommentRepository;
import com.group01.community.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCommentUseCase {
    private final PostRepository posts;
    private final CommentRepository comments;

    @Transactional
    public CommentResult execute(CommunityActor actor, CreateCommentCommand command) {
        CommunityApplicationSupport.requireVisiblePost(posts, command.postId(), actor);
        if (command.parentCommentId() != null) {
            var parent = comments.findByIdForUpdate(command.parentCommentId())
                    .orElseThrow(() -> new CommunityNotFoundException("Comment not found"));
            if (!parent.getPostId().equals(command.postId()) || parent.getStatus() != com.group01.community.domain.vo.ContentStatus.ACTIVE) {
                throw new CommunityException("Parent comment must be active and belong to the same post");
            }
        }
        return CommentResult.from(comments.save(Comment.create(
                command.postId(), actor.userId(), command.parentCommentId(), command.body()
        )));
    }
}
