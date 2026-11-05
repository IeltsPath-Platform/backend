package com.ieltspath.user.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

final class AvatarReferences {
    private AvatarReferences() { }

    static Optional<String> ownedKey(String reference, String projectUrl, String bucket, UUID userId) {
        if (reference == null || projectUrl == null || bucket == null) return Optional.empty();
        try {
            URI actual = URI.create(reference);
            URI base = URI.create(projectUrl);
            if (!actual.getScheme().equalsIgnoreCase(base.getScheme())
                    || !actual.getHost().equalsIgnoreCase(base.getHost())
                    || actual.getPort() != base.getPort()
                    || actual.getRawQuery() != null || actual.getRawFragment() != null
                    || actual.getRawUserInfo() != null) return Optional.empty();
            String prefix = "/storage/v1/object/public/" + bucket + "/";
            String path = actual.getRawPath();
            if (!path.startsWith(prefix)) return Optional.empty();
            String key = path.substring(prefix.length());
            return ObjectKeys.isOwnedBy(key, "images", userId) ? Optional.of(key) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
