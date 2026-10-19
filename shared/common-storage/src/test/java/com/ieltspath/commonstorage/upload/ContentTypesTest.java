package com.ieltspath.commonstorage.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ContentTypesTest {

    @Test
    void stripsParametersAndLowercases() {
        assertThat(ContentTypes.normalize("audio/webm;codecs=opus")).isEqualTo("audio/webm");
        assertThat(ContentTypes.normalize("Audio/WebM ")).isEqualTo("audio/webm");
    }

    @Test
    void mapsAliasesToCanonicalTypes() {
        assertThat(ContentTypes.normalize("audio/x-wav")).isEqualTo("audio/wav");
        assertThat(ContentTypes.normalize("audio/mp3")).isEqualTo("audio/mpeg");
        assertThat(ContentTypes.normalize("audio/x-m4a")).isEqualTo("audio/mp4");
        assertThat(ContentTypes.normalize("image/jpg")).isEqualTo("image/jpeg");
    }

    @Test
    void blankAndNullNormalizeToNull() {
        assertThat(ContentTypes.normalize(null)).isNull();
        assertThat(ContentTypes.normalize("")).isNull();
        assertThat(ContentTypes.normalize("  ;charset=x")).isNull();
    }

    @Test
    void extensionForEverySupportedTypeAndRejectsOthers() {
        assertThat(ContentTypes.extensionFor("image/png")).isEqualTo("png");
        assertThat(ContentTypes.extensionFor("image/jpeg")).isEqualTo("jpg");
        assertThat(ContentTypes.extensionFor("image/webp")).isEqualTo("webp");
        assertThat(ContentTypes.extensionFor("image/gif")).isEqualTo("gif");
        assertThat(ContentTypes.extensionFor("audio/webm")).isEqualTo("webm");
        assertThat(ContentTypes.extensionFor("audio/mp4")).isEqualTo("m4a");
        assertThat(ContentTypes.extensionFor("audio/ogg")).isEqualTo("ogg");
        assertThat(ContentTypes.extensionFor("audio/mpeg")).isEqualTo("mp3");
        assertThat(ContentTypes.extensionFor("audio/wav")).isEqualTo("wav");
        assertThatThrownBy(() -> ContentTypes.extensionFor("image/svg+xml"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
