package com.group01.community.infrastructure.persistence;

import com.group01.community.domain.vo.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostJpaRepository extends JpaRepository<PostJpaEntity, UUID> {
    Page<PostJpaEntity> findByStatus(ContentStatus status, Pageable pageable);
}
