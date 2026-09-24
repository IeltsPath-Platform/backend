package com.group01.community.application.usecase;

import com.group01.community.application.CommunityActor;
import com.group01.community.application.command.AddReactionCommand;
import com.group01.community.domain.repository.PostRepository;
import com.group01.community.domain.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddReactionUseCase {
    private final PostRepository posts;
    private final ReactionRepository reactions;

    @Transactional
    public void execute(CommunityActor actor, AddReactionCommand command) {
        CommunityApplicationSupport.requireVisiblePost(posts, command.postId(), actor);
        reactions.add(command.postId(), actor.userId(), command.type());
    }
}
