package com.group01.community.application.usecase;

import com.group01.community.application.CommunityActor;
import com.group01.community.application.command.CreatePostCommand;
import com.group01.community.application.result.PostResult;
import com.group01.community.domain.aggregate.Post;
import com.group01.community.domain.repository.PostRepository;
import com.group01.community.domain.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePostUseCase {
    private final PostRepository posts;
    private final ReactionRepository reactions;

    @Transactional
    public PostResult execute(CommunityActor actor, CreatePostCommand command) {
        Post saved = posts.save(Post.create(actor.userId(), command.category(), command.title(), command.body()));
        return PostResult.from(saved, reactions.counts(saved.getId()));
    }
}
