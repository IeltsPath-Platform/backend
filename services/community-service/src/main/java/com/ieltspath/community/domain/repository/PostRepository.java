package com.ieltspath.community.domain.repository;

import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.vo.CommunityPage;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;

import java.util.Optional;
import java.util.UUID;

public interface PostRepository {
    Post save(Post post);

    Optional<Post> findById(UUID id);

    CommunityPage<Post> findByStatus(ContentStatus status, int page, int size);

    CommunityPage<Post> findByStatusAndCategory(ContentStatus status, PostCategory category, int page, int size);
}
