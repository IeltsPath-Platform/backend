package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.UnavailableObjectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VocabularyAudioUrlResolverTest {
    private static final String BUCKET = "media";
    private static final String KEY = "vocabulary/pronunciation/uk/democracy.mp3";

    private ObjectStorage objectStorage;
    private VocabularyAudioUrlResolver resolver;

    @BeforeEach
    void setUp() {
        objectStorage = mock(ObjectStorage.class);
        resolver = new VocabularyAudioUrlResolver(objectStorage, BUCKET);
    }

    @Test
    void buildsThePublicUrlOfAnObjectKeyInTheMediaBucket() {
        when(objectStorage.publicUrl(BUCKET, KEY)).thenReturn("https://storage.example/object/public/media/" + KEY);

        assertEquals("https://storage.example/object/public/media/" + KEY, resolver.resolve(KEY));
    }

    @Test
    void keepsAValueThatIsAlreadyAnHttpsUrl() {
        assertEquals("https://cdn.example.com/a.mp3", resolver.resolve("https://cdn.example.com/a.mp3"));
        verifyNoInteractions(objectStorage);
    }

    @Test
    void returnsNullWhenThereIsNoReference() {
        assertNull(resolver.resolve(null));
        assertNull(resolver.resolve(" "));
        verifyNoInteractions(objectStorage);
    }

    @Test
    void returnsNullWhenStorageIsNotConfiguredOrRejectsTheKey() {
        assertNull(new VocabularyAudioUrlResolver(new UnavailableObjectStorage(), BUCKET).resolve(KEY));

        when(objectStorage.publicUrl(BUCKET, "../secret")).thenThrow(new IllegalArgumentException("unsafe key"));
        assertNull(resolver.resolve("../secret"));
    }
}
