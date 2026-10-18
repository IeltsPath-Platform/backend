package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.query.PageQuery;
import com.ieltspath.community.application.result.PageResult;
import com.ieltspath.community.application.result.PostResult;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.repository.ReactionRepository;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.ReactionType;
import com.ieltspath.community.domain.vo.PostCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListPostsUseCase {
    private final PostRepository posts;
    private final ReactionRepository reactions;

    @Transactional(readOnly = true)
    public PageResult<PostResult> execute(PageQuery query) {
        return execute(query, null);
    }

    @Transactional(readOnly = true)
    public PageResult<PostResult> execute(PageQuery query, PostCategory category) {
        var page = category == null
                ? posts.findByStatus(ContentStatus.ACTIVE, query.page(), query.size())
                : posts.findByStatusAndCategory(ContentStatus.ACTIVE, category, query.page(), query.size());
        var postIds = page.items().stream().map(post -> post.getId()).collect(Collectors.toSet());
        Map<UUID, Map<ReactionType, Long>> counts = reactions.counts(postIds);
        return PageResult.from(page, query.page(), query.size(), post ->
                PostResult.from(post, counts.getOrDefault(post.getId(), Map.of())));
    }
}
