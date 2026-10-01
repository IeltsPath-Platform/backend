package com.group01.content.domain.vo;

import com.group01.content.domain.exception.InvalidMediaReferenceException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaReferencePolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://cdn.example.com/charts/roofs.png",
            "data:image/png;base64,iVBORw0KGgo=",
            "data:image/jpeg;base64,/9j/4AAQ",
            "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4="
    })
    void imageUrlsAndImageDataUrisAreReturnedUnchanged(String reference) {
        assertThat(MediaReferencePolicy.resolve(AssetType.IMAGE, reference)).isEqualTo(reference);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "file:///etc/passwd",
            "http://cdn.example.com/roofs.png",
            "/internal/charts/roofs.png",
            "data:text/html;base64,PHNjcmlwdD4=",
            "data:image/svg+xml,<svg onload=alert(1)>",
            "https://cdn.example.com/a b.png"
    })
    void otherReferencesAreRejected(String reference) {
        assertThatThrownBy(() -> MediaReferencePolicy.resolve(AssetType.IMAGE, reference))
                .isInstanceOf(InvalidMediaReferenceException.class);
    }

    @Test
    void nullOrNonImageAssetsAreRejected() {
        assertThatThrownBy(() -> MediaReferencePolicy.resolve(AssetType.IMAGE, null))
                .isInstanceOf(InvalidMediaReferenceException.class);
        assertThatThrownBy(() -> MediaReferencePolicy.resolve(AssetType.AUDIO, "https://cdn.example.com/a.mp3"))
                .isInstanceOf(InvalidMediaReferenceException.class);
    }
}
