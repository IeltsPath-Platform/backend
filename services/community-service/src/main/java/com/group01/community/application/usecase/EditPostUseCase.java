package com.group01.community.application.usecase;

import com.group01.community.application.CommunityActor;
import com.group01.community.application.command.EditPostCommand;
import com.group01.community.application.result.PostResult;
import com.group01.community.domain.exception.CommunityNotFoundException;
import com.group01.community.domain.repository.PostRepository;
import com.group01.community.domain.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditPostUseCase {
    private final PostRepository posts;
    private final ReactionRepository reactions;

    @Transactional
    public PostResult execute(CommunityActor actor, EditPostCommand command) {
        var post = posts.findById(command.postId())
                .orElseThrow(() -> new CommunityNotFoundException("Post not found"));
        post.edit(actor.userId(), command.category(), command.title(), command.body());
        var saved = posts.save(post);
        return PostResult.from(saved, reactions.counts(saved.getId()));
    }
}
