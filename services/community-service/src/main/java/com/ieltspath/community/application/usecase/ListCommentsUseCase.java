package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.query.PageQuery;
import com.ieltspath.community.application.result.CommentResult;
import com.ieltspath.community.application.result.PageResult;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.vo.ContentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCommentsUseCase {
    private final PostRepository posts;
    private final CommentRepository comments;

    @Transactional(readOnly = true)
    public PageResult<CommentResult> execute(
            CommunityActor actor,
            java.util.UUID postId,
            PageQuery query
    ) {
        CommunityApplicationSupport.requireVisiblePost(posts, postId, actor);
        var page = comments.findByPostIdAndStatus(postId, ContentStatus.ACTIVE, query.page(), query.size());
        return PageResult.from(page, query.page(), query.size(), CommentResult::from);
    }
}
