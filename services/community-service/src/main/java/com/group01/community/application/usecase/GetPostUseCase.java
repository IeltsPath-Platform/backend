package com.group01.community.application.usecase;

import com.group01.community.application.CommunityActor;
import com.group01.community.application.result.PostResult;
import com.group01.community.domain.repository.PostRepository;
import com.group01.community.domain.repository.ReactionRepository;
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
