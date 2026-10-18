package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.UserDisplayNameReader;
import com.ieltspath.user.application.result.UserDisplayNameResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserDisplayNamesUseCase {
    private static final int MAX_IDS = 50;
    private final UserDisplayNameReader reader;

    @Transactional(readOnly = true)
    public List<UserDisplayNameResult> execute(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty() || userIds.size() > MAX_IDS
                || userIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("ids must contain between 1 and 50 user IDs");
        }
        List<UUID> distinctIds = List.copyOf(new LinkedHashSet<>(userIds));
        Map<UUID, String> namesById = reader.findDisplayNamesByIds(distinctIds);
        return distinctIds.stream()
                .filter(namesById::containsKey)
                .map(id -> new UserDisplayNameResult(id, namesById.get(id)))
                .toList();
    }
}
