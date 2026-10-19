package com.ieltspath.community.infrastructure.config;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.community.application.port.MediaPolicy;
import com.ieltspath.community.domain.exception.CommunityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;

@Component
public class AllowedMediaPolicy implements MediaPolicy {

    private final Set<String> imageHosts;
    private final Set<String> videoHosts;
    private final String bucket;
    private final String supabaseHost;
    private final String supabaseScheme;
    private final int supabasePort;

    public AllowedMediaPolicy(
            @Value("${community.media.image-hosts:images.unsplash.com,i.imgur.com}") String images,
            @Value("${community.media.video-hosts:youtube.com,www.youtube.com,youtu.be,loom.com,www.loom.com,drive.google.com}") String videos,
            @Value("${community.media.bucket:community}") String bucket,
            @Value("${app.storage.supabase.url:}") String supabaseUrl
    ) {
        this.imageHosts = hosts(images);
        this.videoHosts = hosts(videos);
        this.bucket = bucket != null ? bucket.trim() : "community";

        if (supabaseUrl != null && !supabaseUrl.isBlank()) {
            URI u = URI.create(supabaseUrl.trim());
            this.supabaseHost = u.getHost() != null ? u.getHost().toLowerCase(Locale.ROOT) : null;
            this.supabaseScheme = u.getScheme() != null ? u.getScheme().toLowerCase(Locale.ROOT) : null;
            this.supabasePort = u.getPort();
        } else {
            this.supabaseHost = null;
            this.supabaseScheme = null;
            this.supabasePort = -1;
        }
    }

    private static Set<String> hosts(String text) {
        return new HashSet<>(Arrays.stream(text.split(","))
                .map(String::trim)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .toList());
    }

    @Override
    public void validate(UUID authorId, List<String> images, String video) {
        if (images != null) {
            images.forEach(url -> validateImage(authorId, url));
        }
        if (video != null) {
            validateVideo(video);
        }
    }

    private void validateImage(UUID authorId, String value) {
        URI u = parseAndCheckBasic(value);
        if (isSupabaseHost(u)) {
            String prefix = "/storage/v1/object/public/" + bucket + "/";
            String path = u.getPath() != null ? u.getPath() : "";
            if (!path.startsWith(prefix)) {
                throw new CommunityException("Invalid storage media URL");
            }
            String key = path.substring(prefix.length());
            if (!ObjectKeys.isOwnedBy(key, IMAGE_KEY_PREFIX, authorId)) {
                throw new CommunityException("Image does not belong to the author");
            }
        } else {
            if (!imageHosts.contains(u.getHost().toLowerCase(Locale.ROOT))) {
                throw new CommunityException("Media URL must use HTTPS and an allowed host");
            }
        }
    }

    /** Videos are never stored by the service: only HTTPS links to an allowed video host are accepted. */
    private void validateVideo(String value) {
        URI u = parseAndCheckBasic(value);
        if (isSupabaseHost(u) || !videoHosts.contains(u.getHost().toLowerCase(Locale.ROOT))) {
            throw new CommunityException("Media URL must use HTTPS and an allowed host");
        }
    }

    private URI parseAndCheckBasic(String value) {
        try {
            if (value == null || value.length() > 500) {
                throw new IllegalArgumentException();
            }
            URI u = URI.create(value);
            if (u.getHost() == null || u.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
            if (isSupabaseHost(u)) {
                if (supabaseScheme != null && !supabaseScheme.equalsIgnoreCase(u.getScheme())) {
                    throw new IllegalArgumentException();
                }
            } else {
                if (!"https".equalsIgnoreCase(u.getScheme()) || (u.getPort() != -1 && u.getPort() != 443)) {
                    throw new IllegalArgumentException();
                }
            }
            return u;
        } catch (RuntimeException e) {
            if (e instanceof CommunityException) {
                throw e;
            }
            throw new CommunityException("Media URL must use HTTPS and an allowed host");
        }
    }

    private boolean isSupabaseHost(URI u) {
        if (supabaseHost == null || u.getHost() == null) {
            return false;
        }
        if (!u.getHost().equalsIgnoreCase(supabaseHost)) {
            return false;
        }
        if (supabasePort != -1 && u.getPort() != supabasePort) {
            return false;
        }
        return true;
    }
}
