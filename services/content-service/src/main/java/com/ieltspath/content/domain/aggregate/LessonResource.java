package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.LessonResourceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * A slide deck or document of a lesson. The file lives outside the platform, so only its http(s) link is kept. The
 * type is fixed at creation; title, link and order can change. Order is unique per lesson and type.
 */
public class LessonResource {
    static final int MAX_URL_LENGTH = 2_000;

    private final UUID id;
    private final UUID lessonId;
    private final LessonResourceType resourceType;
    private String title;
    private String url;
    private int sortOrder;
    private final Instant createdAt;
    private Instant updatedAt;

    public LessonResource(UUID id, UUID lessonId, LessonResourceType resourceType, String title, String url,
                          int sortOrder, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.lessonId = Objects.requireNonNull(lessonId, "lessonId must not be null");
        if (resourceType == null) throw new IllegalArgumentException("resourceType is required");
        this.resourceType = resourceType;
        this.title = requireTitle(title);
        this.url = requireUrl(url);
        this.sortOrder = requireSortOrder(sortOrder);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static LessonResource create(UUID lessonId, LessonResourceType resourceType, String title, String url,
                                        int sortOrder) {
        Instant now = Instant.now();
        return new LessonResource(UUID.randomUUID(), lessonId, resourceType, title, url, sortOrder, now, now);
    }

    /** Used by the reorder use case, which assigns each resource of one type its position in the new sequence. */
    public void changeSortOrder(int sortOrder) {
        this.sortOrder = requireSortOrder(sortOrder);
        this.updatedAt = Instant.now();
    }

    public void update(String title, String url, int sortOrder) {
        String checkedTitle = requireTitle(title);
        String checkedUrl = requireUrl(url);
        int checkedOrder = requireSortOrder(sortOrder);
        this.title = checkedTitle;
        this.url = checkedUrl;
        this.sortOrder = checkedOrder;
        this.updatedAt = Instant.now();
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        String trimmed = title.trim();
        if (trimmed.length() > 255) throw new IllegalArgumentException("title exceeds 255 characters");
        return trimmed;
    }

    /** Only absolute http(s) links with a host: no javascript:, data: or relative links reach a learner's browser. */
    private static String requireUrl(String url) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("url must not be blank");
        String trimmed = url.trim();
        if (trimmed.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("url exceeds " + MAX_URL_LENGTH + " characters");
        }
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
                throw new IllegalArgumentException("url must be an absolute http or https link");
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("url must be an absolute http or https link");
        }
        return trimmed;
    }

    private static int requireSortOrder(int sortOrder) {
        if (sortOrder < 0) throw new IllegalArgumentException("sortOrder must not be negative");
        return sortOrder;
    }

    public UUID getId() { return id; }
    public UUID getLessonId() { return lessonId; }
    public LessonResourceType getResourceType() { return resourceType; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
