package com.ieltspath.library.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ieltspath.library.domain.repository.WordlyVocabularyRepository;
import com.ieltspath.library.api.controller.InternalWordlyController;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;

class WordlyRequestLimitsTest {
    @Test void rejectsOversizedRequestsBeforeRepositoryAndAtHttpBoundary() {
        var repository=mock(WordlyVocabularyRepository.class);
        var useCase=new GetRandomWordlyWordsUseCase(repository);
        assertThrows(IllegalArgumentException.class,()->useCase.execute(1000000));
        assertThrows(IllegalArgumentException.class,()->useCase.execute(0));
        useCase.execute(100);
        verify(repository).random(100);
        verifyNoMoreInteractions(repository);
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(new InternalWordlyController.Request(101)).isEmpty());
            assertTrue(factory.getValidator().validate(new InternalWordlyController.Request(100)).isEmpty());
        }
        assertEquals("Page limit must be 1-500",assertThrows(IllegalArgumentException.class,()->useCase.page("",501)).getMessage());
    }
}
