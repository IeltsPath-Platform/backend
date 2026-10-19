package com.ieltspath.community.infrastructure.config;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.community.domain.exception.CommunityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AllowedMediaPolicyTest {

    private AllowedMediaPolicy policy;

    private static final String SUPABASE_URL = "https://example.supabase.co";
    private static final String BUCKET = "community";

    @BeforeEach
    void setUp() {
        policy = new AllowedMediaPolicy(
                "images.unsplash.com,i.imgur.com",
                "youtube.com,www.youtube.com,youtu.be,loom.com,www.loom.com,drive.google.com",
                BUCKET,
                SUPABASE_URL
        );
    }

    @Test
    void allowsExternalWhitelistedImage() {
        UUID authorId = UUID.randomUUID();
        assertDoesNotThrow(() -> policy.validate(authorId, List.of("https://images.unsplash.com/photo-123"), null));
    }

    @Test
    void allowsExternalWhitelistedVideo() {
        UUID authorId = UUID.randomUUID();
        assertDoesNotThrow(() -> policy.validate(authorId, null, "https://youtube.com/watch?v=123"));
    }

    @Test
    void rejectsDisallowedExternalImageHost() {
        UUID authorId = UUID.randomUUID();
        CommunityException ex = assertThrows(CommunityException.class,
                () -> policy.validate(authorId, List.of("https://evil.com/pic.jpg"), null));
        assertEquals("Media URL must use HTTPS and an allowed host", ex.getMessage());
    }

    @Test
    void rejectsDisallowedExternalVideoHost() {
        UUID authorId = UUID.randomUUID();
        CommunityException ex = assertThrows(CommunityException.class,
                () -> policy.validate(authorId, null, "https://vimeo.com/123"));
        assertEquals("Media URL must use HTTPS and an allowed host", ex.getMessage());
    }

    @Test
    void rejectsNonHttpsUrl() {
        UUID authorId = UUID.randomUUID();
        CommunityException ex = assertThrows(CommunityException.class,
                () -> policy.validate(authorId, List.of("http://images.unsplash.com/pic.jpg"), null));
        assertEquals("Media URL must use HTTPS and an allowed host", ex.getMessage());
    }

    @Test
    void allowsSupabaseImageOwnedByAuthor() {
        UUID authorId = UUID.randomUUID();
        String key = ObjectKeys.forUser("community/post", authorId, "jpg");
        String url = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/" + key;

        assertDoesNotThrow(() -> policy.validate(authorId, List.of(url), null));
    }

    @Test
    void rejectsSupabaseImageOwnedByAnotherUser() {
        UUID authorId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        String key = ObjectKeys.forUser("community/post", otherUser, "jpg");
        String url = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/" + key;

        CommunityException ex = assertThrows(CommunityException.class,
                () -> policy.validate(authorId, List.of(url), null));
        assertEquals("Image does not belong to the author", ex.getMessage());
    }

    @Test
    void rejectsSupabaseImageWithInvalidPrefixOrPathTraversal() {
        UUID authorId = UUID.randomUUID();
        String url = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/other/" + authorId + "/test.jpg";

        CommunityException ex = assertThrows(CommunityException.class,
                () -> policy.validate(authorId, List.of(url), null));
        assertEquals("Image does not belong to the author", ex.getMessage());
    }

    @Test
    void rejectsSupabaseStorageUrlAsVideo() {
        UUID authorId = UUID.randomUUID();
        String key = ObjectKeys.forUser("community/post", authorId, "mp4");
        String url = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/" + key;

        CommunityException ex = assertThrows(CommunityException.class,
                () -> policy.validate(authorId, null, url));
        assertEquals("Media URL must use HTTPS and an allowed host", ex.getMessage());
    }
}

