package com.ieltspath.content.application.usecase;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.content.domain.exception.InvalidMediaReferenceException;
import com.ieltspath.content.domain.vo.AccessTier;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MediaUrlResolverTest {
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private final MediaUrlResolver resolver = new MediaUrlResolver(
            new MediaReferencePolicy("https://media.example.test/"), storage, "media-premium",
            Duration.ofSeconds(900));

    @Test
    void premiumLessonAudioUsesPrivateBucketAndFifteenMinuteSignature() {
        String signed = "https://private.example.test/object/sign/media-premium/listening/demo/ls1.mp3?token=secret";
        when(storage.signedDownloadUrl("media-premium", "listening/demo/ls1.mp3", Duration.ofSeconds(900)))
                .thenReturn(signed);

        assertThat(resolver.resolve(AssetType.AUDIO, "listening/demo/ls1.mp3", AccessTier.PREMIUM, true))
                .isEqualTo(signed);
        verify(storage).signedDownloadUrl("media-premium", "listening/demo/ls1.mp3", Duration.ofSeconds(900));
    }

    @Test
    void publicAudioKeepsItsPublicUrlEvenForSignedLessonRequests() {
        assertThat(resolver.resolve(AssetType.AUDIO, "listening/demo/ls2.mp3", AccessTier.PUBLIC, true))
                .isEqualTo("https://media.example.test/listening/demo/ls2.mp3");
        verifyNoInteractions(storage);
    }

    @Test
    void premiumAudioOutsideLessonReadKeepsTheExistingPublicResolverContract() {
        assertThat(resolver.resolve(AssetType.AUDIO, "listening/demo/ls1.mp3", AccessTier.PREMIUM, false))
                .isEqualTo("https://media.example.test/listening/demo/ls1.mp3");
        verifyNoInteractions(storage);
    }

    @Test
    void signedAudioRejectsMalformedKeysBeforeCallingStorage() {
        assertThatThrownBy(() -> resolver.resolve(AssetType.AUDIO, "../secret.mp3", AccessTier.PREMIUM, true))
                .isInstanceOf(InvalidMediaReferenceException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void premiumHttpsReferenceIsPreservedWithoutSigning() {
        String url = "https://cdn.example.test/audio.mp3";
        assertThat(resolver.resolve(AssetType.AUDIO, url, AccessTier.PREMIUM, true)).isEqualTo(url);
        verifyNoInteractions(storage);
    }
}

