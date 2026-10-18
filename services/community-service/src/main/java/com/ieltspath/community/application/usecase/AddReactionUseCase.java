package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.AddReactionCommand;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.repository.ReactionRepository;
import com.ieltspath.community.application.port.CommunityEventOutbox;
import com.ieltspath.community.application.port.CommunityEventTypes;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddReactionUseCase {
    private final PostRepository posts;
    private final ReactionRepository reactions;
    private final CommunityEventOutbox outbox;

    @Transactional
    public void execute(CommunityActor actor, AddReactionCommand command) {
        var post = posts.findById(command.postId())
                .orElseThrow(() -> new CommunityNotFoundException("Post not found"));
        if (post.getStatus() != com.ieltspath.community.domain.vo.ContentStatus.ACTIVE && !actor.administrator()) {
            CommunityApplicationSupport.requireVisiblePost(posts, command.postId(), actor);
        }
        boolean changed = reactions.add(command.postId(), actor.userId(), command.type());
        if (changed && !actor.userId().equals(post.getAuthorId())) {
            outbox.record(CommunityEventTypes.POST_REACTED, actor.userId(), post.getAuthorId(), post.getId(), null);
        }
    }
}
