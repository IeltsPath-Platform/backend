package com.ieltspath.content.domain.vo;

import com.ieltspath.content.domain.exception.InvalidMediaReferenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaReferencePolicyTest {
    private final MediaReferencePolicy policy = new MediaReferencePolicy("https://media.example.test/ieltspath");
    private final MediaReferencePolicy noBase = new MediaReferencePolicy("");

    @ParameterizedTest
    @ValueSource(strings = {
            "https://cdn.example.com/charts/roofs.png",
            "data:image/png;base64,iVBORw0KGgo=",
            "data:image/jpeg;base64,/9j/4AAQ",
            "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4="
    })
    void imageUrlsAndImageDataUrisAreReturnedUnchanged(String reference) {
        assertThat(noBase.resolve(AssetType.IMAGE, reference)).isEqualTo(reference);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "file:///etc/passwd",
            "http://cdn.example.com/roofs.png",
            "/internal/charts/roofs.png",
            "data:text/html;base64,PHNjcmlwdD4=",
            "data:image/svg+xml,<svg onload=alert(1)>",
            "https://cdn.example.com/a b.png",
            "charts/roofs.png"
    })
    void otherImageReferencesAreRejected(String reference) {
        assertThatThrownBy(() -> policy.resolve(AssetType.IMAGE, reference))
                .isInstanceOf(InvalidMediaReferenceException.class);
    }

    @Test
    void audioKeyIsJoinedToTheBaseWithExactlyOneSlash() {
        assertThat(policy.resolve(AssetType.AUDIO, "listening/demo/ls1.mp3"))
                .isEqualTo("https://media.example.test/ieltspath/listening/demo/ls1.mp3");
        assertThat(new MediaReferencePolicy("https://media.example.test/ieltspath/")
                .resolve(AssetType.AUDIO, "listening/demo/ls1.mp3"))
                .isEqualTo("https://media.example.test/ieltspath/listening/demo/ls1.mp3");
        assertThat(policy.resolve(AssetType.AUDIO, "https://other.example.com/a.mp3"))
                .isEqualTo("https://other.example.com/a.mp3");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../x.mp3", "listening/../../x.mp3", "/x.mp3", "javascript:alert(1)", "file:///x.mp3",
            "data:audio/mpeg;base64,AAAA", "listening//x.mp3"
    })
    void unsafeAudioReferencesAreRejected(String reference) {
        assertThatThrownBy(() -> policy.resolve(AssetType.AUDIO, reference))
                .isInstanceOf(InvalidMediaReferenceException.class);
    }

    @Test
    void audioKeyWithoutABaseUrlIsRejectedInsteadOfReturningABrokenUrl() {
        assertThatThrownBy(() -> noBase.resolve(AssetType.AUDIO, "listening/demo/ls1.mp3"))
                .isInstanceOf(InvalidMediaReferenceException.class);
        assertThatThrownBy(() -> noBase.resolve(AssetType.AUDIO, null))
                .isInstanceOf(InvalidMediaReferenceException.class);
    }

    @Test
    void baseUrlMustBeHttps() {
        assertThatThrownBy(() -> new MediaReferencePolicy("http://media.example.test/"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
