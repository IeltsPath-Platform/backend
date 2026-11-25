package com.ieltspath.user.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourcePasswordBlocklistTest {
    @Test
    void loadsCuratedPasswordsAndMatchesWithoutCaseSensitivity() {
        var blocklist = new ResourcePasswordBlocklist();
        assertThat(blocklist.contains("PASSWORD")).isTrue();
        assertThat(blocklist.contains("matkhau123")).isTrue();
        assertThat(blocklist.contains("R4ndom!x")).isFalse();
    }
}
