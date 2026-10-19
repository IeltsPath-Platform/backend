package com.ieltspath.commonstorage.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObjectKeysTest {

    private static final UUID USER = UUID.fromString("0b1f2a3c-4d5e-4f60-8172-93a4b5c6d7e8");
    private static final UUID OTHER = UUID.fromString("11111111-2222-4333-8444-555555555555");
    private static final String GENERATED = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Test
    void generatesOwnedKeysThatPassRequireOwnedBy() {
        String key = ObjectKeys.forUser("speaking", USER, "webm");
        assertThat(key).matches("^speaking/0b1f2a3c-4d5e-4f60-8172-93a4b5c6d7e8/" + GENERATED + "\\.webm$");
        assertThat(ObjectKeys.forUser("speaking", USER, "webm")).isNotEqualTo(key);
        ObjectKeys.requireOwnedBy(key, "speaking", USER);
        assertThat(ObjectKeys.isOwnedBy(key, "speaking", USER)).isTrue();
    }

    @Test
    void generatesUnownedKeysAndAcceptsMultiSegmentPrefix() {
        assertThat(ObjectKeys.of("listening/practice", "mp3")).matches("^listening/practice/" + GENERATED + "\\.mp3$");
    }

    @Test
    void rejectsBadPrefixesAndExtensionsWhenGenerating() {
        assertThatThrownBy(() -> ObjectKeys.of("Speaking", "webm")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ObjectKeys.of("a/b/c/d", "webm")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ObjectKeys.of("x".repeat(33), "webm")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ObjectKeys.of("speaking", "WEBM")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ObjectKeys.of("speaking", "we bm")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ObjectKeys.of("speaking", "toolong")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireSafeAcceptsLegacySeedKeysWithUppercase() {
        ObjectKeys.requireSafe("listening/demo/numM.mp3");
        ObjectKeys.requireSafe("listening/practice/pt-listening-01-s1.mp3");
    }

    @Test
    void requireSafeRejectsUnsafeKeys() {
        String[] unsafe = {
                "", "/a/b", "a//b", "a/../b", "a/b.", "a/b c", "a/é", "a/%2e%2e/b", "a/b?x",
                "a/b/c/d/e/f/g/h/i", "a/" + "x".repeat(101), "x".repeat(301), "a\\b"
        };
        for (String key : unsafe) {
            assertThatThrownBy(() -> ObjectKeys.requireSafe(key)).isInstanceOf(InvalidObjectKeyException.class);
        }
        assertThatThrownBy(() -> ObjectKeys.requireSafe(null)).isInstanceOf(InvalidObjectKeyException.class);
    }

    @Test
    void requireOwnedByRejectsOtherOwnersAndShapes() {
        String mine = "speaking/" + USER + "/" + UUID.randomUUID() + ".webm";
        String theirs = "speaking/" + OTHER + "/" + UUID.randomUUID() + ".webm";
        assertThat(ObjectKeys.isOwnedBy(theirs, "speaking", USER)).isFalse();
        assertThat(ObjectKeys.isOwnedBy(mine, "avatar", USER)).isFalse();
        assertThat(ObjectKeys.isOwnedBy(mine + "/x", "speaking", USER)).isFalse();
        assertThat(ObjectKeys.isOwnedBy("speaking/" + USER + "/a.webm", "speaking", USER)).isFalse();
        assertThat(ObjectKeys.isOwnedBy("speaking-x/" + USER + "/" + UUID.randomUUID() + ".webm", "speaking", USER))
                .isFalse();
        assertThat(ObjectKeys.isOwnedBy("speaking/" + USER.toString().toUpperCase() + "/"
                + UUID.randomUUID() + ".webm", "speaking", USER)).isFalse();
        assertThat(ObjectKeys.isOwnedBy("../etc", "speaking", USER)).isFalse();
    }

    @Test
    void requireOwnedByNeverEchoesTheKey() {
        String mine = "speaking/" + USER + "/bad-name.webm";
        assertThatThrownBy(() -> ObjectKeys.requireOwnedBy(mine, "speaking", USER))
                .isInstanceOf(InvalidObjectKeyException.class)
                .hasMessage("object key is invalid")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(USER.toString()));
    }
}
