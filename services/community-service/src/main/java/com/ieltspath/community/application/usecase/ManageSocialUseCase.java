package com.ieltspath.community.application.usecase;

import com.ieltspath.events.community.CommunityEventTypes;
import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.port.*;
import com.ieltspath.community.domain.repository.*;
import com.ieltspath.community.domain.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
@Service
@RequiredArgsConstructor
public class ManageSocialUseCase {
 private final SocialRepository social;
 private final PostRepository posts;
 private final PublicProfileClient profiles;
 private final CommunityEventOutbox outbox;
 private final TransactionTemplate transaction;
 public void follow(CommunityActor actor,UUID target) {
 if(actor.userId().equals(target))throw new CommunityException("Cannot follow yourself");
 // The remote profile check runs before the transaction so a slow user-service never holds a connection.
 if(!profiles.findProfiles(Set.of(target)).containsKey(target))throw new CommunityNotFoundException("User not found");
 transaction.executeWithoutResult(status -> {
 if(social.follow(actor.userId(),target))outbox.record(CommunityEventTypes.USER_FOLLOWED,actor.userId(),target,null,null);
 });
 }
 @Transactional public void unfollow(CommunityActor actor,UUID target){social.unfollow(actor.userId(),target);}
 @Transactional public void bookmark(CommunityActor actor,UUID post){CommunityApplicationSupport.requireVisiblePost(posts,post,actor);social.bookmark(actor.userId(),post);}
 @Transactional public void unbookmark(CommunityActor actor,UUID post){social.unbookmark(actor.userId(),post);}
}
