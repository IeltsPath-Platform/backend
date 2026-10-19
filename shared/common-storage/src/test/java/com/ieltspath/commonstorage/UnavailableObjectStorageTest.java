package com.ieltspath.commonstorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class UnavailableObjectStorageTest {

    private final ObjectStorage storage = new UnavailableObjectStorage();

    private void assertUnavailable(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(StorageException.class)
                .satisfies(e -> assertThat(((StorageException) e).reason())
                        .isEqualTo(StorageException.Reason.UNAVAILABLE));
    }

    @Test
    void everyOperationReportsUnavailable() {
        assertUnavailable(() -> storage.put("avatars", "a/b", new byte[]{1}, "image/png"));
        assertUnavailable(() -> storage.delete("avatars", "a/b"));
        assertUnavailable(() -> storage.exists("avatars", "a/b"));
        assertUnavailable(() -> storage.publicUrl("avatars", "a/b"));
        assertUnavailable(() -> storage.signedDownloadUrl("speaking-recordings", "a/b", Duration.ofMinutes(1)));
        assertUnavailable(() -> storage.signedUploadUrl("media", "a/b"));
    }
}
