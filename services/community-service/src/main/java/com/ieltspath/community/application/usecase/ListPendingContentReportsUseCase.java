package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.application.query.PageQuery;
import com.ieltspath.community.application.result.ContentReportQueueItemResult;
import com.ieltspath.community.application.result.PageResult;
import com.ieltspath.community.domain.repository.ContentReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListPendingContentReportsUseCase {
    private final ContentReportRepository reports;
    private final PublicProfileClient profiles;

    @Transactional(readOnly = true)
    public PageResult<ContentReportQueueItemResult> execute(CommunityActor actor, PageQuery query) {
        CommunityApplicationSupport.requireAdministrator(actor);
        var page = reports.findPending(query.page(), query.size());
        var ids = new LinkedHashSet<UUID>();
        page.items().forEach(entry -> {
            ids.add(entry.reporterId());
            if (entry.contentAuthorId() != null) ids.add(entry.contentAuthorId());
        });
        var profilesById = new java.util.HashMap<UUID, com.ieltspath.community.application.result.PublicProfile>();
        var allIds = new ArrayList<>(ids);
        for (int start = 0; start < allIds.size(); start += 100) {
            var batch = new LinkedHashSet<>(allIds.subList(start, Math.min(start + 100, allIds.size())));
            Map<UUID, com.ieltspath.community.application.result.PublicProfile> found = profiles.findProfiles(batch);
            profilesById.putAll(found);
        }
        return PageResult.from(page, query.page(), query.size(), entry -> ContentReportQueueItemResult.from(entry,
                displayName(profilesById, entry.reporterId()),
                entry.contentAuthorId() == null ? null : displayName(profilesById, entry.contentAuthorId())));
    }

    private static String displayName(Map<UUID, com.ieltspath.community.application.result.PublicProfile> profiles,
                                      UUID id) {
        var profile = profiles.get(id);
        return profile == null ? null : profile.displayName();
    }
}
