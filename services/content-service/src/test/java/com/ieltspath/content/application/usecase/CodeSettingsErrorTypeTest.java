package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CodeSetting;
import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.service.CodePattern;
import com.ieltspath.content.domain.vo.ErrorCategory;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class CodeSettingsErrorTypeTest {
    private final CodeSettingsStore store = mock(CodeSettingsStore.class);
    private final ErrorTypeRepository errorTypes = mock(ErrorTypeRepository.class);
    private final CreateErrorTypeUseCase createErrorType = mock(CreateErrorTypeUseCase.class);
    private final CodeSettingsUseCases useCases = new CodeSettingsUseCases(store, new CodePattern(),
            mock(CourseRepository.class), mock(TopicRepository.class), null, null, null, null,
            new CodeContextResolver(mock(CourseRepository.class), mock(TopicRepository.class), mock(LessonAuthoringRepository.class)),
            new CodeExistence(null, null, null, null, null, errorTypes), null, createErrorType);

    @Test
    void suggestsACodeFromTheNameAndCreatesWithIt() {
        when(store.find("ERROR_TYPE")).thenReturn(Optional.of(setting(true, true)));
        when(createErrorType.execute(anyString(), anyString(), any(), anyInt(), isNull(), anyString()))
                .thenReturn(ErrorType.create("COLLOCATION", "Collocation", ErrorCategory.GRAMMAR, 1, null, "hint"));

        assertEquals("COLLOCATION", useCases.suggest("ERROR_TYPE", null, null, null, null, null, null, null, null,
                "Collocation").code());

        useCases.createErrorType("", "Collocation", ErrorCategory.GRAMMAR, 1, null, "hint");
        verify(createErrorType).execute(eq("COLLOCATION"), eq("Collocation"), eq(ErrorCategory.GRAMMAR), eq(1), isNull(), eq("hint"));
    }

    @Test
    void foldsVietnameseLettersIncludingDStroke() {
        when(store.find("ERROR_TYPE")).thenReturn(Optional.of(setting(true, true)));
        assertEquals("DAO_NGU", useCases.suggest("ERROR_TYPE", null, null, null, null, null, null, null, null,
                "Đảo ngữ").code());
    }

    @Test
    void startsWithALetterAndStaysWithinSixtyCharacters() {
        when(store.find("ERROR_TYPE")).thenReturn(Optional.of(setting(true, true)));
        assertEquals("E_2ND_CONDITIONAL", useCases.suggest("ERROR_TYPE", null, null, null, null, null, null, null, null,
                "2nd conditional").code());
        String code = useCases.suggest("ERROR_TYPE", null, null, null, null, null, null, null, null,
                "A very long error name that keeps going on and on until it passes sixty characters").code();
        assertTrue(code.length() <= 60, code);
        assertTrue(code.matches("^[A-Z][A-Z0-9_]*$"), code);
        assertFalse(code.endsWith("_"));
    }

    @Test
    void givesNoSuggestionWhenTheNameHasNoLettersOrDigits() {
        when(store.find("ERROR_TYPE")).thenReturn(Optional.of(setting(true, true)));
        assertEquals("", useCases.suggest("ERROR_TYPE", null, null, null, null, null, null, null, null, "!!!").code());
        assertEquals("", useCases.suggest("ERROR_TYPE", null, null, null, null, null, null, null, null, "").code());
        assertEquals("", useCases.suggest("ERROR_TYPE", null, null, null, null, null, null, null, null, null).code());
    }

    @Test
    void appendsANumberWhenTheCodeIsTaken() {
        when(store.find("ERROR_TYPE")).thenReturn(Optional.of(setting(true, true)));
        when(errorTypes.existsByCode("COLLOCATION")).thenReturn(true);
        when(errorTypes.existsByCode("COLLOCATION_2")).thenReturn(true);

        assertEquals("COLLOCATION_3", useCases.suggest("ERROR_TYPE", null, null, null, null, null, null, null, null,
                "Collocation").code());
    }

    @Test
    void keepsASuffixedCodeWithinSixtyCharacters() {
        String name = "a".repeat(60);
        when(store.find("ERROR_TYPE")).thenReturn(Optional.of(setting(true, true)));
        when(errorTypes.existsByCode("A".repeat(60))).thenReturn(true);

        String code = useCases.suggest("ERROR_TYPE", null, null, null, null, null, null, null, null, name).code();
        assertEquals(60, code.length());
        assertTrue(code.endsWith("_2"), code);
    }

    @Test
    void keepsACodeTheAuthorTypedWhenEditable() {
        when(store.find("ERROR_TYPE")).thenReturn(Optional.of(setting(true, true)));
        when(createErrorType.execute(anyString(), anyString(), any(), anyInt(), isNull(), anyString()))
                .thenReturn(ErrorType.create("MY_CODE", "Collocation", ErrorCategory.GRAMMAR, 1, null, "hint"));

        useCases.createErrorType("MY_CODE", "Collocation", ErrorCategory.GRAMMAR, 1, null, "hint");
        verify(createErrorType).execute(eq("MY_CODE"), anyString(), any(), anyInt(), isNull(), anyString());
    }

    @Test
    void lockedModeRejectsADifferentCode() {
        when(store.find("ERROR_TYPE")).thenReturn(Optional.of(setting(true, false)));
        var error = assertThrows(InvalidContentStateException.class, () -> useCases.createErrorType(
                "MY_CODE", "Collocation", ErrorCategory.GRAMMAR, 1, null, "hint"));
        assertEquals(InvalidContentStateException.CODE_LOCKED, error.getCode());
    }

    @Test
    void whenNotAppliedTheCodeMustBeTyped() {
        when(store.find("ERROR_TYPE")).thenReturn(Optional.of(setting(false, true)));
        var error = assertThrows(IllegalArgumentException.class, () -> useCases.createErrorType(
                "", "Collocation", ErrorCategory.GRAMMAR, 1, null, "hint"));
        assertTrue(error.getMessage().contains("Mã lỗi"));
        assertEquals("", useCases.suggest("ERROR_TYPE", null, null, null, null, null, null, null, null, "Collocation").code());

        useCases.createErrorType("TYPED", "Collocation", ErrorCategory.GRAMMAR, 1, null, "hint");
        verify(createErrorType).execute(eq("TYPED"), anyString(), any(), anyInt(), isNull(), anyString());
    }

    @Test
    void listsErrorTypeSettingsForTheAdminPage() {
        when(store.find("ERROR_TYPE")).thenReturn(Optional.of(setting(false, true)));

        var listed = useCases.listConnected();

        assertEquals(1, listed.size());
        assertEquals("ERROR_TYPE", listed.getFirst().kind());
        assertFalse(listed.getFirst().applied());
        assertEquals("{NAME}", listed.getFirst().pattern());
        assertTrue(listed.getFirst().editable());
    }

    @Test
    void refusesToCreateWhenTheNameYieldsNoCode() {
        when(store.find("ERROR_TYPE")).thenReturn(Optional.of(setting(true, true)));
        var error = assertThrows(IllegalArgumentException.class, () -> useCases.createErrorType(
                "", "!!!", ErrorCategory.GRAMMAR, 1, null, "hint"));
        assertTrue(error.getMessage().contains("mã lỗi"));
        verifyNoInteractions(createErrorType);
    }

    private static CodeSetting setting(boolean applied, boolean editable) {
        return new CodeSetting("ERROR_TYPE", applied, "{NAME}", 1, editable);
    }
}
