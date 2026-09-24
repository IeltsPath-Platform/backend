package com.group01.community.domain.repository;

import com.group01.community.domain.aggregate.Post;
import com.group01.community.domain.vo.CommunityPage;
import com.group01.community.domain.vo.ContentStatus;

import java.util.Optional;
import java.util.UUID;

public interface PostRepository {
    Post save(Post post);

    Optional<Post> findById(UUID id);

    CommunityPage<Post> findByStatus(ContentStatus status, int page, int size);
}
