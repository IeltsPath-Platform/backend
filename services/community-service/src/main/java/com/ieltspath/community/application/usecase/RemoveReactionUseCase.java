package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.RemoveReactionCommand;
import com.ieltspath.community.domain.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemoveReactionUseCase {
    private final ReactionRepository reactions;

    @Transactional
    public void execute(CommunityActor actor, RemoveReactionCommand command) {
        reactions.remove(command.postId(), actor.userId(), command.type());
    }
}
