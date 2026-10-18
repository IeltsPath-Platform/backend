package com.ieltspath.community.domain.vo;
import java.util.UUID;
import java.time.Instant;
public record FollowEntry(UUID userId,Instant createdAt) {}
