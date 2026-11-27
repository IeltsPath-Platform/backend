package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.application.query.PageQuery;
import com.ieltspath.community.application.result.PageResult;
import com.ieltspath.community.application.result.PublicProfile;
import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.SocialRepository;
import com.ieltspath.community.domain.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageBlocksUseCase {
    private final UserBlockRepository blocks;
    private final SocialRepository social;
    private final PublicProfileClient profiles;
    private final TransactionTemplate transaction;

    public void block(CommunityActor actor, UUID targetUserId) {
        if (actor.userId().equals(targetUserId)) {
            throw new CommunityException("Bạn không thể tự chặn chính mình");
        }
        if (!profiles.findProfiles(Set.of(targetUserId)).containsKey(targetUserId)) {
            throw new CommunityNotFoundException("Không tìm thấy người dùng");
        }
        transaction.executeWithoutResult(status -> {
            blocks.block(actor.userId(), targetUserId);
            social.unfollowBothWays(actor.userId(), targetUserId);
        });
    }

    public void unblock(CommunityActor actor, UUID targetUserId) {
        if (actor.userId().equals(targetUserId)) {
            throw new CommunityException("Bạn không thể tự bỏ chặn chính mình");
        }
        transaction.executeWithoutResult(status -> blocks.unblock(actor.userId(), targetUserId));
    }

    public PageResult<PublicProfile> listBlocked(CommunityActor actor, PageQuery query) {
        var page = blocks.findBlockedUsers(actor.userId(), query.page(), query.size());
        if (page.items().isEmpty()) {
            return PageResult.from(page, query.page(), query.size(), PublicProfile::anonymous);
        }
        var found = profiles.findProfiles(new LinkedHashSet<>(page.items()));
        return PageResult.from(page, query.page(), query.size(),
                id -> found.getOrDefault(id, PublicProfile.anonymous(id)));
    }
}
