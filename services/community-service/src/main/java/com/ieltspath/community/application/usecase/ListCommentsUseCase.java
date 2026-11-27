package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.query.PageQuery;
import com.ieltspath.community.application.result.CommentResult;
import com.ieltspath.community.application.result.PageResult;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.repository.UserBlockRepository;
import com.ieltspath.community.domain.vo.ContentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCommentsUseCase {
    private final PostRepository posts;
    private final CommentRepository comments;
    private final UserBlockRepository blocks;

    @Autowired
    public ListCommentsUseCase(PostRepository posts, CommentRepository comments, UserBlockRepository blocks) {
        this.posts = posts;
        this.comments = comments;
        this.blocks = blocks;
    }

    public ListCommentsUseCase(PostRepository posts, CommentRepository comments) {
        this(posts, comments, CommunityApplicationSupport.noOpBlocks());
    }

    @Transactional(readOnly = true)
    public PageResult<CommentResult> execute(
            CommunityActor actor,
            java.util.UUID postId,
            PageQuery query
    ) {
        var post = CommunityApplicationSupport.requireVisiblePost(posts, postId, actor);
        if (blocks.isBlockedEitherWay(actor.userId(), post.getAuthorId())) {
            throw new CommunityNotFoundException("Không tìm thấy bài viết");
        }
        var page = comments.findByPostIdAndStatusVisibleTo(postId, ContentStatus.ACTIVE, actor.userId(), query.page(), query.size());
        return PageResult.from(page, query.page(), query.size(), CommentResult::from);
    }
}
