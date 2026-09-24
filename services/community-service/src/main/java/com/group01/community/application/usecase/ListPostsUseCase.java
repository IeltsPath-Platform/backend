package com.group01.community.application.usecase;

import com.group01.community.application.query.PageQuery;
import com.group01.community.application.result.PageResult;
import com.group01.community.application.result.PostResult;
import com.group01.community.domain.repository.PostRepository;
import com.group01.community.domain.repository.ReactionRepository;
import com.group01.community.domain.vo.ContentStatus;
import com.group01.community.domain.vo.ReactionType;
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
        var page = posts.findByStatus(ContentStatus.ACTIVE, query.page(), query.size());
        var postIds = page.items().stream().map(post -> post.getId()).collect(Collectors.toSet());
        Map<UUID, Map<ReactionType, Long>> counts = reactions.counts(postIds);
        return PageResult.from(page, query.page(), query.size(), post ->
                PostResult.from(post, counts.getOrDefault(post.getId(), Map.of())));
    }
}
