package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.result.PostResult;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPostUseCase {
    private final PostRepository posts;
    private final ReactionRepository reactions;

    @Transactional(readOnly = true)
    public PostResult execute(UUID postId, CommunityActor actor) {
        var post = CommunityApplicationSupport.requireVisiblePost(posts, postId, actor);
        return PostResult.from(post, reactions.counts(postId));
    }
}
