package com.ieltspath.community.infrastructure.adapter;
import com.ieltspath.community.domain.repository.SocialRepository;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
@Repository
@RequiredArgsConstructor
public class SocialRepositoryAdapter implements SocialRepository {
 private final JdbcTemplate jdbc;
 public boolean follow(UUID actor,UUID target){return jdbc.update("INSERT INTO user_follows(follower_id,followee_id) VALUES (?,?) ON CONFLICT DO NOTHING",actor,target)>0;}
 public void unfollow(UUID actor,UUID target){jdbc.update("DELETE FROM user_follows WHERE follower_id=? AND followee_id=?",actor,target);}
 public void bookmark(UUID actor,UUID post){jdbc.update("INSERT INTO post_bookmarks(user_id,post_id) VALUES (?,?) ON CONFLICT DO NOTHING",actor,post);}
 public void unbookmark(UUID actor,UUID post){jdbc.update("DELETE FROM post_bookmarks WHERE user_id=? AND post_id=?",actor,post);}
}
