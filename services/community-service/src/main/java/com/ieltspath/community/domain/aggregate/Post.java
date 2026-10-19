package com.ieltspath.community.domain.aggregate;

import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.exception.CommunityForbiddenException;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostBackground;
import com.ieltspath.community.domain.vo.PostCategory;

import java.time.Instant;
import java.util.UUID;

public class Post {
    private final UUID id;
    private final UUID authorId;
    private final Instant createdAt;
    private PostCategory category;
    private String title;
    private String body;
    private ContentStatus status;
    private Instant updatedAt;
    private Long version;
    private java.util.List<String> imageUrls = java.util.List.of();
    private String videoUrl;
    private java.math.BigDecimal targetBand;
    private PostBackground background;
    public PostBackground getBackground() { return background; }
    /**
     * A background is only allowed on short text-only posts. Call it after the body and media are final,
     * because it validates the post as it will be stored.
     */
    public void setBackground(PostBackground value) {
        if (value != null) {
            if (!imageUrls.isEmpty() || videoUrl != null) throw new CommunityException("A background cannot be combined with images or video");
            if (body.length() > PostBackground.MAX_BODY_LENGTH)
                throw new CommunityException("A post with a background may contain at most " + PostBackground.MAX_BODY_LENGTH + " characters");
        }
        background = value;
    }
    public java.util.List<String> getImageUrls() { return imageUrls; }
    public String getVideoUrl() { return videoUrl; }
    public java.math.BigDecimal getTargetBand() { return targetBand; }
    public void setMedia(java.util.List<String> images, String video, java.math.BigDecimal band) {
        if (images != null && images.size() > 4) throw new CommunityException("At most four images are allowed");
        if (band != null && (band.signum() < 0 || band.compareTo(java.math.BigDecimal.valueOf(9)) > 0
                || band.remainder(new java.math.BigDecimal("0.5")).signum() != 0)) {
            throw new CommunityException("Target band must be between 0 and 9 in half-band steps");
        }
        imageUrls = images == null ? java.util.List.of() : java.util.List.copyOf(images);
        imageUrls.forEach(Post::requireHttps);
        if (video != null) requireHttps(video);
        videoUrl = video; targetBand = band;
    }
    private static void requireHttps(String value) {
        try {
            var uri = java.net.URI.create(value);
            if (value.length() > 500 || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) throw new IllegalArgumentException();
        } catch (RuntimeException e) { throw new CommunityException("Media must be a valid HTTPS URL"); }
    }


    public Post(
            UUID id,
            UUID authorId,
            PostCategory category,
            String title,
            String body,
            ContentStatus status,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        if (id == null || authorId == null || category == null || status == null
                || createdAt == null || updatedAt == null) {
            throw new CommunityException("Post fields must not be null");
        }
        this.id = id;
        this.authorId = authorId;
        this.category = category;
        this.title = normalizeTitle(title);
        this.body = requireBody(body);
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Post create(UUID authorId, PostCategory category, String title, String body) {
        Instant now = Instant.now();
        return new Post(UUID.randomUUID(), authorId, category, title, body, ContentStatus.ACTIVE, now, now, null);
    }

    private static String normalizeTitle(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.length() > 200) {
            throw new CommunityException("Title is too long");
        }
        return v;
    }

    private static String requireBody(String value) {
        if (value == null || value.isBlank()) {
            throw new CommunityException("Body is required");
        }
        String v = value.trim();
        if (v.length() > 20000) {
            throw new CommunityException("Body is too long");
        }
        return v;
    }

    public void edit(UUID actor, PostCategory category, String title, String body) {
        requireAuthor(actor);
        requireActive();
        if (category == null) {
            throw new CommunityException("Category is required");
        }
        this.category = category;
        this.title = normalizeTitle(title);
        this.body = requireBody(body);
        this.updatedAt = Instant.now();
    }

    public void delete(UUID actor) { delete(actor, false); }
    public void delete(UUID actor, boolean administrator) {
        if (!administrator) requireAuthor(actor);
        requireActive();
        status = ContentStatus.DELETED;
        updatedAt = Instant.now();
    }

    public void moderate(ContentStatus target) {
        if (target != ContentStatus.HIDDEN && target != ContentStatus.ACTIVE) {
            throw new CommunityException("Moderation status must be ACTIVE or HIDDEN");
        }
        if (status == ContentStatus.DELETED) {
            throw new CommunityException("Deleted post cannot be moderated");
        }
        status = target;
        updatedAt = Instant.now();
    }

    private void requireAuthor(UUID actor) {
        if (!authorId.equals(actor)) {
            throw new CommunityForbiddenException("Only the author may change this post");
        }
    }

    private void requireActive() {
        if (status != ContentStatus.ACTIVE) {
            throw new CommunityException("Only active posts may be changed");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public PostCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
