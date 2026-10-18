package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.CreatePostCommand;
import com.ieltspath.community.application.result.PostResult;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePostUseCase {
    private final PostRepository posts;
    private final ReactionRepository reactions;
    private final com.ieltspath.community.application.port.MediaPolicy mediaPolicy;

    @Transactional
    public PostResult execute(CommunityActor actor, CreatePostCommand command) {
        mediaPolicy.validate(command.imageUrls(), command.videoUrl());
        Post post = Post.create(actor.userId(), command.category(), command.title(), command.body());
        post.setMedia(command.imageUrls(), command.videoUrl(), command.targetBand());
        post.setBackground(command.background());
        Post saved = posts.save(post);
        return PostResult.from(saved, reactions.counts(saved.getId()));
    }
}
