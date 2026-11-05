package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.LessonResourceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * A slide deck or document of a lesson. It is either an http(s) link or, for a document, a file kept in storage and
 * referenced by its key; never both. The type and the file are fixed at creation; title, link and order can change.
 * Order is unique per lesson and type.
 */
public class LessonResource {
    static final int MAX_URL_LENGTH = 2_000;
    static final int MAX_FILE_KEY_LENGTH = 300;
    /** Every uploaded lesson document is stored under this prefix, whoever uploaded it. */
    public static final String FILE_KEY_PREFIX = "lesson-document";

    private final UUID id;
    private final UUID lessonId;
    private final LessonResourceType resourceType;
    private String title;
    private String url;
    private final String fileKey;
    private int sortOrder;
    private final Instant createdAt;
    private Instant updatedAt;

    public LessonResource(UUID id, UUID lessonId, LessonResourceType resourceType, String title, String url,
                          String fileKey, int sortOrder, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.lessonId = Objects.requireNonNull(lessonId, "lessonId must not be null");
        if (resourceType == null) throw new IllegalArgumentException("resourceType is required");
        this.resourceType = resourceType;
        this.title = requireTitle(title);
        boolean hasFile = fileKey != null && !fileKey.isBlank();
        if (hasFile == (url != null && !url.isBlank())) {
            throw new IllegalArgumentException("a resource needs either a url or a file, not both");
        }
        if (hasFile && resourceType != LessonResourceType.DOCUMENT) {
            throw new IllegalArgumentException("only a document can be an uploaded file");
        }
        this.url = hasFile ? null : requireUrl(url);
        this.fileKey = hasFile ? requireFileKey(fileKey) : null;
        this.sortOrder = requireSortOrder(sortOrder);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static LessonResource create(UUID lessonId, LessonResourceType resourceType, String title, String url,
                                        int sortOrder) {
        Instant now = Instant.now();
        return new LessonResource(UUID.randomUUID(), lessonId, resourceType, title, url, null, sortOrder, now, now);
    }

    /** A document whose file was uploaded to storage. The caller has already checked who owns the key. */
    public static LessonResource createFile(UUID lessonId, String title, String fileKey, int sortOrder) {
        Instant now = Instant.now();
        return new LessonResource(UUID.randomUUID(), lessonId, LessonResourceType.DOCUMENT, title, null, fileKey,
                sortOrder, now, now);
    }

    /** Used by the reorder use case, which assigns each resource of one type its position in the new sequence. */
    public void changeSortOrder(int sortOrder) {
        this.sortOrder = requireSortOrder(sortOrder);
        this.updatedAt = Instant.now();
    }

    /** A file resource keeps its file, so it takes no link; to change the file, delete the resource and add a new one. */
    public void update(String title, String url, int sortOrder) {
        String checkedTitle = requireTitle(title);
        if (fileKey != null && url != null && !url.isBlank()) {
            throw new IllegalArgumentException("an uploaded file cannot be replaced by a link");
        }
        String checkedUrl = fileKey != null ? null : requireUrl(url);
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

    private static String requireFileKey(String fileKey) {
        String trimmed = fileKey.trim();
        if (trimmed.length() > MAX_FILE_KEY_LENGTH || !trimmed.startsWith(FILE_KEY_PREFIX + "/")) {
            throw new IllegalArgumentException("fileKey is not a lesson document key");
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
    /** The link, or {@code null} when the resource is an uploaded file. */
    public String getUrl() { return url; }
    /** The storage key of the uploaded file, or {@code null} when the resource is a link. */
    public String getFileKey() { return fileKey; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
