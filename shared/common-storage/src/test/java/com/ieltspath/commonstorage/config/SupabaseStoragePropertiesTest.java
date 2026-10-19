package com.ieltspath.commonstorage.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SupabaseStoragePropertiesTest {

    private static final String KEY = "test-key-not-real";

    private static SupabaseStorageProperties props(String url, String key) {
        return new SupabaseStorageProperties(url, key, null, null, null);
    }

    @Test
    void unconfiguredWhenBothBlank() {
        assertThat(props(null, null).isConfigured()).isFalse();
        assertThat(props("  ", "").isConfigured()).isFalse();
    }

    @Test
    void rejectsOnlyOneOfUrlOrKeyWithoutEchoingValue() {
        assertThatThrownBy(() -> props("https://abc.supabase.co", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.storage.supabase.url and app.storage.supabase.secret-key must be configured together");
        assertThatThrownBy(() -> props(null, KEY))
                .hasMessageNotContaining(KEY);
    }

    @Test
    void acceptsHttpsAndLoopbackHttpOnly() {
        assertThat(props("https://abc.supabase.co", KEY).url()).isEqualTo("https://abc.supabase.co");
        assertThat(props("http://localhost:54321", KEY).url()).isEqualTo("http://localhost:54321");
        assertThat(props("http://127.0.0.1:54321", KEY).url()).isEqualTo("http://127.0.0.1:54321");
        assertThatThrownBy(() -> props("http://example.com", KEY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checksHostNotPrefix() {
        assertThatThrownBy(() -> props("http://localhost@evil.com", KEY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> props("http://localhost.evil.com", KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUserInfoQueryFragmentAndPaths() {
        assertThatThrownBy(() -> props("https://user:pw@abc.supabase.co", KEY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> props("https://abc.supabase.co/path", KEY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> props("https://abc.supabase.co?x=1", KEY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> props("https://abc.supabase.co/#f", KEY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> props("https://abc.supabase.co/storage/v1", KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stripsTrailingSlash() {
        assertThat(props("https://abc.supabase.co/", KEY).url()).isEqualTo("https://abc.supabase.co");
    }

    @Test
    void toStringNeverContainsKey() {
        assertThat(props("https://abc.supabase.co", KEY).toString()).doesNotContain(KEY).contains("secretKey=***");
    }

    @Test
    void timeoutsDefaultAndMustBePositive() {
        SupabaseStorageProperties defaults = props("https://abc.supabase.co", KEY);
        assertThat(defaults.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(defaults.readTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(defaults.uploadTimeout()).isEqualTo(Duration.ofSeconds(120));
        assertThatThrownBy(() -> new SupabaseStorageProperties("https://abc.supabase.co", KEY,
                Duration.ZERO, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SupabaseStorageProperties("https://abc.supabase.co", KEY,
                null, Duration.ofSeconds(-1), null)).isInstanceOf(IllegalArgumentException.class);
    }
}
