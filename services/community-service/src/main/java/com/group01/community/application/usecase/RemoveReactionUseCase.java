package com.group01.community.application.usecase;

import com.group01.community.application.CommunityActor;
import com.group01.community.application.command.RemoveReactionCommand;
import com.group01.community.domain.repository.ReactionRepository;
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
