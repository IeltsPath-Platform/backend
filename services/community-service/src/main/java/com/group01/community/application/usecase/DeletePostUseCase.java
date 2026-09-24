package com.group01.community.application.usecase;

import com.group01.community.application.CommunityActor;
import com.group01.community.application.command.DeletePostCommand;
import com.group01.community.domain.exception.CommunityNotFoundException;
import com.group01.community.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletePostUseCase {
    private final PostRepository posts;

    @Transactional
    public void execute(CommunityActor actor, DeletePostCommand command) {
        var post = posts.findById(command.postId())
                .orElseThrow(() -> new CommunityNotFoundException("Post not found"));
        post.delete(actor.userId());
        posts.save(post);
    }
}
