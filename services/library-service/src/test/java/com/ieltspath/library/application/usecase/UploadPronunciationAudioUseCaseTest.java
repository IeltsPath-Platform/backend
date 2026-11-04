package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.library.application.port.CatalogMediaStorage;
import com.ieltspath.library.application.result.StoredImageResult;
import com.ieltspath.library.domain.exception.DuplicateCodeException;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UploadPronunciationAudioUseCaseTest {
    private static final byte[] MP3 = {'I', 'D', '3', 4, 0, 0, 0, 0, 0, 0};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    private CatalogMediaStorage storage;
    private VocabularyRepository vocabulary;
    private UploadPronunciationAudioUseCase useCase;

    @BeforeEach
    void setUp() {
        storage = mock(CatalogMediaStorage.class);
        vocabulary = mock(VocabularyRepository.class);
        useCase = new UploadPronunciationAudioUseCase(storage, vocabulary);
        when(storage.store(anyString(), any(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            byte[] bytes = invocation.getArgument(1);
            return new StoredImageResult(key, "https://storage.example/" + key, "audio/mpeg", bytes.length);
        });
    }

    @Test
    void storesTheFileUnderTheAccentAndTheSlugOfTheWord() {
        when(vocabulary.existsByNormalizedLemma("green roof")).thenReturn(false);

        StoredImageResult result = useCase.execute("Green Roof", "us", "audio/mpeg", MP3);

        assertEquals("vocabulary/pronunciation/us/green-roof.mp3", result.key());
        verify(storage).store("vocabulary/pronunciation/us/green-roof.mp3", MP3, "audio/mpeg");
    }

    @Test
    void usesTheUkFolderForBritishPronunciation() {
        when(vocabulary.existsByNormalizedLemma("climate")).thenReturn(false);

        assertEquals("vocabulary/pronunciation/uk/climate.mp3",
                useCase.execute("climate", "uk", "audio/mpeg", MP3).key());
    }

    @Test
    void refusesAWordThatAlreadyExistsWithoutTouchingStorage() {
        when(vocabulary.existsByNormalizedLemma("climate")).thenReturn(true);

        assertThrows(DuplicateCodeException.class, () -> useCase.execute("climate", "us", "audio/mpeg", MP3));
        verifyNoInteractions(storage);
    }

    @Test
    void refusesAnAccentOtherThanUsOrUk() {
        assertThrows(InvalidDataException.class, () -> useCase.execute("climate", "au", "audio/mpeg", MP3));
        verifyNoInteractions(storage);
    }

    @Test
    void refusesALemmaWithNoLettersOrDigitsToNameTheFile() {
        assertThrows(InvalidDataException.class, () -> useCase.execute("  !! ", "us", "audio/mpeg", MP3));
        verifyNoInteractions(storage);
    }

    @Test
    void refusesBytesThatAreNotTheDeclaredAudio() {
        when(vocabulary.existsByNormalizedLemma("climate")).thenReturn(false);

        assertThrows(UploadRejectedException.class, () -> useCase.execute("climate", "us", "audio/mpeg", PNG));
        verifyNoInteractions(storage);
    }
}
