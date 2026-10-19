package com.ieltspath.commonstorage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StorageExceptionTest {

    @Test
    void messageIsFixedFormatWithoutKeyOrBucket() {
        StorageException e = new StorageException("put", StorageException.Reason.ALREADY_EXISTS, 409, false,
                StorageException.ref("avatars", "user-123/secret.png"));
        assertThat(e.getMessage()).startsWith("storage put failed: reason=ALREADY_EXISTS status=409 outcomeUnknown=false ref=");
        assertThat(e.getMessage()).doesNotContain("avatars").doesNotContain("user-123").doesNotContain("secret.png");
        assertThat(e.getCause()).isNull();
    }

    @Test
    void refIsStableEightHexAndDependsOnBucketAndKey() {
        String a = StorageException.ref("avatars", "a/b");
        assertThat(a).matches("[0-9a-f]{8}");
        assertThat(StorageException.ref("avatars", "a/b")).isEqualTo(a);
        assertThat(StorageException.ref("avatars", "a/c")).isNotEqualTo(a);
        assertThat(StorageException.ref("media", "a/b")).isNotEqualTo(a);
    }

    @Test
    void exposesOutcomeUnknownForCallers() {
        StorageException e = new StorageException("put", StorageException.Reason.UNAVAILABLE, 0, true, "0000abcd");
        assertThat(e.outcomeUnknown()).isTrue();
        assertThat(e.reason()).isEqualTo(StorageException.Reason.UNAVAILABLE);
        assertThat(e.status()).isZero();
    }

    @Test
    void isNotIllegalArgumentException() {
        assertThat(new StorageException("put", StorageException.Reason.UNEXPECTED_RESPONSE, 200, false, "-"))
                .isNotInstanceOf(IllegalArgumentException.class);
    }
}
