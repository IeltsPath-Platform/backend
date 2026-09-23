package com.group01.community.domain.aggregate;

import com.group01.community.domain.exception.CommunityException;
import com.group01.community.domain.exception.CommunityForbiddenException;
import com.group01.community.domain.vo.ContentStatus;
import com.group01.community.domain.vo.PostCategory;

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

    public void delete(UUID actor) {
        requireAuthor(actor);
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
