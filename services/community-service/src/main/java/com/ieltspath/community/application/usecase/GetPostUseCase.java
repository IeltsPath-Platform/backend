package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.result.PostResult;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.repository.ReactionRepository;
import com.ieltspath.community.domain.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetPostUseCase {
    private final PostRepository posts;
    private final ReactionRepository reactions;
    private final UserBlockRepository blocks;

    @Autowired
    public GetPostUseCase(PostRepository posts, ReactionRepository reactions, UserBlockRepository blocks) {
        this.posts = posts;
        this.reactions = reactions;
        this.blocks = blocks;
    }

    public GetPostUseCase(PostRepository posts, ReactionRepository reactions) {
        this(posts, reactions, CommunityApplicationSupport.noOpBlocks());
    }

    @Transactional(readOnly = true)
    public PostResult execute(UUID postId, CommunityActor actor) {
        var post = CommunityApplicationSupport.requireVisiblePost(posts, postId, actor);
        if (blocks.isBlockedEitherWay(actor.userId(), post.getAuthorId())) {
            throw new CommunityNotFoundException("Không tìm thấy bài viết");
        }
        return PostResult.from(post, reactions.counts(postId));
    }
}
