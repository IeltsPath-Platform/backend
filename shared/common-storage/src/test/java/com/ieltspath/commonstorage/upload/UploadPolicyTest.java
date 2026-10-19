package com.ieltspath.commonstorage.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.commonstorage.UploadRejectedException.Reason;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class UploadPolicyTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};

    private final UploadPolicy policy = UploadPolicy.of(10, "image/png", "image/jpeg", "image/webp");

    private static Reason reasonOf(Runnable call) {
        try {
            call.run();
            throw new AssertionError("expected UploadRejectedException");
        } catch (UploadRejectedException e) {
            return e.reason();
        }
    }

    @Test
    void checkReturnsCanonicalTypeForMatchingContent() {
        assertThat(policy.check(PNG, "image/png;charset=x")).isEqualTo("image/png");
    }

    @Test
    void checkRejectsEmptyTooLargeWrongTypeAndMismatch() {
        assertThat(reasonOf(() -> policy.check(new byte[0], "image/png"))).isEqualTo(Reason.EMPTY);
        assertThat(reasonOf(() -> policy.check(null, "image/png"))).isEqualTo(Reason.EMPTY);
        assertThat(reasonOf(() -> policy.check(new byte[11], "image/png"))).isEqualTo(Reason.TOO_LARGE);
        assertThat(reasonOf(() -> policy.check(PNG, "audio/webm"))).isEqualTo(Reason.TYPE_NOT_ALLOWED);
        assertThat(reasonOf(() -> policy.check(PNG, null))).isEqualTo(Reason.TYPE_NOT_ALLOWED);
        assertThat(reasonOf(() -> policy.check(JPEG, "image/png"))).isEqualTo(Reason.CONTENT_MISMATCH);
        assertThat(reasonOf(() -> policy.check(new byte[]{1, 2, 3}, "image/png"))).isEqualTo(Reason.CONTENT_MISMATCH);
    }

    @Test
    void requireSizeAllowsBoundaryAndRejectsAbove() {
        policy.requireSize(10);
        assertThat(reasonOf(() -> policy.requireSize(11))).isEqualTo(Reason.TOO_LARGE);
    }

    @Test
    void readBoundedStopsAtLimitPlusOne() throws IOException {
        assertThat(policy.readBounded(new ByteArrayInputStream(new byte[10]))).hasSize(10);

        CountingInfinite infinite = new CountingInfinite();
        assertThat(reasonOf(() -> {
            try {
                policy.readBounded(infinite);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        })).isEqualTo(Reason.TOO_LARGE);
        assertThat(infinite.read).isLessThanOrEqualTo(11);
    }

    @Test
    void ofRejectsWildcardsAndEmptyPolicy() {
        assertThatThrownBy(() -> UploadPolicy.of(10, "audio/*")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UploadPolicy.of(10, "*/*")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UploadPolicy.of(10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UploadPolicy.of(0, "image/png")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectionMessagesNeverCarryContent() {
        try {
            policy.check(JPEG, "image/png");
        } catch (UploadRejectedException e) {
            assertThat(e.getMessage()).doesNotContain("ÿ").doesNotContain("image/jpeg");
        }
    }

    /** Stream vô hạn đếm số byte đã được đọc, để kiểm không đọc quá giới hạn. */
    private static final class CountingInfinite extends InputStream {
        long read;

        @Override
        public int read() {
            read++;
            return 0x41;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            int n = Math.min(len, 4096);
            for (int i = 0; i < n; i++) {
                b[off + i] = 0x41;
            }
            read += n;
            return n;
        }
    }
}
