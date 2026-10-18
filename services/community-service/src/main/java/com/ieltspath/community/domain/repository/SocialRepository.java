package com.ieltspath.community.domain.repository;
import java.util.UUID;
public interface SocialRepository {
 boolean follow(UUID actor,UUID target);
 void unfollow(UUID actor,UUID target);
 void bookmark(UUID actor,UUID post);
 void unbookmark(UUID actor,UUID post);
}
