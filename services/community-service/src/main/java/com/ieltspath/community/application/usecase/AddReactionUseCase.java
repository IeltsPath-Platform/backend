package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.AddReactionCommand;
import com.ieltspath.community.domain.exception.CommunityForbiddenException;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.repository.ReactionRepository;
import com.ieltspath.community.domain.repository.UserBlockRepository;
import com.ieltspath.community.application.port.CommunityEventOutbox;
import com.ieltspath.events.community.CommunityEventTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddReactionUseCase {
    private final PostRepository posts;
    private final ReactionRepository reactions;
    private final CommunityEventOutbox outbox;
    private final UserBlockRepository blocks;

    @Autowired
    public AddReactionUseCase(PostRepository posts, ReactionRepository reactions, CommunityEventOutbox outbox,
                              UserBlockRepository blocks) {
        this.posts = posts;
        this.reactions = reactions;
        this.outbox = outbox;
        this.blocks = blocks;
    }

    public AddReactionUseCase(PostRepository posts, ReactionRepository reactions, CommunityEventOutbox outbox) {
        this(posts, reactions, outbox, CommunityApplicationSupport.noOpBlocks());
    }

    @Transactional
    public void execute(CommunityActor actor, AddReactionCommand command) {
        var post = posts.findById(command.postId())
                .orElseThrow(() -> new CommunityNotFoundException("Không tìm thấy bài viết"));
        if (post.getStatus() != com.ieltspath.community.domain.vo.ContentStatus.ACTIVE && !actor.administrator()) {
            CommunityApplicationSupport.requireVisiblePost(posts, command.postId(), actor);
        }
        if (blocks.isBlockedEitherWay(actor.userId(), post.getAuthorId())) {
            throw new CommunityForbiddenException("Không thể tương tác do có thiết lập chặn giữa hai người dùng", "USER_BLOCKED");
        }
        boolean changed = reactions.add(command.postId(), actor.userId(), command.type());
        if (changed && !actor.userId().equals(post.getAuthorId())) {
            outbox.record(CommunityEventTypes.POST_REACTED, actor.userId(), post.getAuthorId(), post.getId(), null);
        }
    }
}
