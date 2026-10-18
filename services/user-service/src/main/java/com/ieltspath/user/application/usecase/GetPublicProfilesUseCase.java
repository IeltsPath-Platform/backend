package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.result.PublicProfileResult;
import com.ieltspath.user.domain.exception.LearnerProfileNotFoundException;
import com.ieltspath.user.domain.repository.PublicProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPublicProfilesUseCase {
    private final PublicProfileRepository repository;

    public PublicProfileResult execute(UUID viewerId, UUID userId) {
        return executeBatch(viewerId, List.of(Objects.requireNonNull(userId))).stream().findFirst()
                .orElseThrow(() -> new LearnerProfileNotFoundException("Public profile not found"));
    }

    public List<PublicProfileResult> executeBatch(UUID viewerId, List<UUID> userIds) {
        Objects.requireNonNull(viewerId, "Authenticated viewer is required");
        if (userIds == null || userIds.size() > 100 || userIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("userIds must contain at most 100 non-null UUIDs");
        }
        if (userIds.isEmpty()) return List.of();
        var ids = new LinkedHashSet<>(userIds);
        var profiles = new LinkedHashMap<UUID, PublicProfileResult>();
        repository.findByUserIds(ids).stream().filter(profile -> profile.visibleTo(viewerId)).forEach(profile ->
                profiles.put(profile.id(), new PublicProfileResult(profile.id(), profile.resolvedDisplayName(viewerId),
                        avatarUrl(profile.avatarReference()), profile.bio(), profile.targetBand(),
                        profile.streak() == null ? 0 : profile.streak())));
        return ids.stream().map(profiles::get).filter(Objects::nonNull).toList();
    }

    public List<PublicProfileResult> search(UUID viewerId, String query, int limit) {
        Objects.requireNonNull(viewerId, "Authenticated viewer is required");
        if (query == null || query.trim().length() < 2 || query.trim().length() > 80 || limit < 1 || limit > 10)
            throw new IllegalArgumentException("query must contain 2 to 80 characters and limit must be 1 to 10");
        return repository.searchVisibleByName(viewerId, query.trim(), limit).stream()
                .map(profile -> new PublicProfileResult(profile.id(), profile.resolvedDisplayName(viewerId),
                        avatarUrl(profile.avatarReference()), profile.bio(), profile.targetBand(),
                        profile.streak() == null ? 0 : profile.streak())).toList();
    }

    private String avatarUrl(String reference) {
        if (reference == null || reference.isBlank()) return null;
        try {
            URI uri = URI.create(reference.trim());
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null ? uri.toString() : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
