package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateKnowledgePointCommand;
import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CodeSetting;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.service.CodePattern;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CodeSettingsKnowledgePointTest {
    private final CodeSettingsStore store = mock(CodeSettingsStore.class);
    private final KnowledgePointRepository knowledgePoints = mock(KnowledgePointRepository.class);
    private final CreateKnowledgePointUseCase createKnowledgePoint = mock(CreateKnowledgePointUseCase.class);
    private final CodeSettingsUseCases useCases = new CodeSettingsUseCases(store, new CodePattern(),
            mock(CourseRepository.class), mock(TopicRepository.class), null, null, null, null,
            new CodeContextResolver(mock(CourseRepository.class), mock(TopicRepository.class),
                    mock(com.ieltspath.content.domain.repository.LessonAuthoringRepository.class)),
            new CodeExistence(null, null, null, null, knowledgePoints), createKnowledgePoint);

    @Test
    void suggestsAndCreatesValidCodeWithKnowledgePointContext() {
        when(store.find("KNOWLEDGE_POINT")).thenReturn(Optional.of(setting(true, true)));
        when(store.peek("KNOWLEDGE_POINT", "", 1)).thenReturn(1L);
        when(knowledgePoints.existsByCode(anyString())).thenReturn(false);

        var suggestion = useCases.suggest("KNOWLEDGE_POINT", null, null, Skill.READING,
                null, null, null, KnowledgePointKind.STRATEGY, 55, "Read short texts");
        assertEquals("R55_READ_SHORT_TEXTS", suggestion.code());

        useCases.createKnowledgePoint(command("", "Read short texts", KnowledgePointKind.STRATEGY, Skill.READING), 55);
        verify(createKnowledgePoint).execute(argThat(command -> command.code().equals("R55_READ_SHORT_TEXTS")));
    }

    @Test
    void returnsEmptySuggestionWhenRenderedCodeDoesNotPassKnowledgePointRules() {
        when(store.find("KNOWLEDGE_POINT")).thenReturn(Optional.of(setting(true, true)));
        when(store.peek("KNOWLEDGE_POINT", "", 1)).thenReturn(1L);
        var shortName = useCases.suggest("KNOWLEDGE_POINT", null, null, Skill.READING,
                null, null, null, KnowledgePointKind.STRATEGY, 55, "A");
        var incompatiblePrefix = useCases.suggest("KNOWLEDGE_POINT", null, null, Skill.ALL,
                null, null, null, KnowledgePointKind.STRATEGY, 55, "All skills");
        assertEquals("", shortName.code());
        assertEquals("", incompatiblePrefix.code());
        assertEquals("", useCases.suggest("KNOWLEDGE_POINT", null, null, Skill.READING,
                null, null, null, KnowledgePointKind.STRATEGY, 55, "").code());
        assertEquals("", useCases.suggest("KNOWLEDGE_POINT", null, null, Skill.READING,
                null, null, null, KnowledgePointKind.STRATEGY, 55, "A very long name that is repeated to exceed the allowed sixty characters").code());
    }

    @Test
    void appendsNumericSuffixWhenKnowledgePointCodeAlreadyExists() {
        when(store.find("KNOWLEDGE_POINT")).thenReturn(Optional.of(setting(true, true)));
        when(store.peek("KNOWLEDGE_POINT", "", 1)).thenReturn(1L);
        when(knowledgePoints.existsByCode("R55_READ_SHORT_TEXTS")).thenReturn(true);
        when(knowledgePoints.existsByCode("R55_READ_SHORT_TEXTS_2")).thenReturn(false);

        assertEquals("R55_READ_SHORT_TEXTS_2", useCases.suggest("KNOWLEDGE_POINT", null, null, Skill.READING,
                null, null, null, KnowledgePointKind.STRATEGY, 55, "Read short texts").code());
    }

    @Test
    void lockedModeRejectsDifferentCodeAndUnappliedModeRequiresManualCode() {
        when(store.find("KNOWLEDGE_POINT")).thenReturn(Optional.of(setting(true, false)));
        when(store.peek("KNOWLEDGE_POINT", "", 1)).thenReturn(1L);
        when(knowledgePoints.existsByCode(anyString())).thenReturn(false);
        var locked = assertThrows(InvalidContentStateException.class, () -> useCases.createKnowledgePoint(
                command("MANUAL", "Read short texts", KnowledgePointKind.STRATEGY, Skill.READING), 55));
        assertEquals(InvalidContentStateException.CODE_LOCKED, locked.getCode());

        when(store.find("KNOWLEDGE_POINT")).thenReturn(Optional.of(setting(false, true)));
        var missingCode = assertThrows(IllegalArgumentException.class, () -> useCases.createKnowledgePoint(
                command("", "Read short texts", KnowledgePointKind.STRATEGY, Skill.READING), 55));
        assertTrue(missingCode.getMessage().contains("Mã điểm kiến thức"));
    }

    @Test
    void rejectsUnsupportedCodeLevel() {
        when(store.find("KNOWLEDGE_POINT")).thenReturn(Optional.of(setting(true, true)));
        var error = assertThrows(IllegalArgumentException.class, () -> useCases.suggest("KNOWLEDGE_POINT", null,
                null, Skill.READING, null, null, null, KnowledgePointKind.STRATEGY, 56, "Reading"));
        assertTrue(error.getMessage().contains("Mức band"));
    }

    private static CodeSetting setting(boolean applied, boolean editable) {
        return new CodeSetting("KNOWLEDGE_POINT", applied, "{PREFIX}{LEVEL}_{NAME}", 1, editable);
    }

    private static CreateKnowledgePointCommand command(String code, String name, KnowledgePointKind kind, Skill skill) {
        return new CreateKnowledgePointCommand(java.util.UUID.randomUUID(), code, name, kind,
                com.ieltspath.content.domain.vo.LearningType.CONCEPT, skill, null,
                null, "Can do this", null, null);
    }
}
