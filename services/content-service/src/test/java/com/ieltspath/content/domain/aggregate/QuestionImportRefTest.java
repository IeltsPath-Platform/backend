package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.QuestionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionImportRefTest {

    @Test
    void blankReferenceMeansNone() {
        assertNull(Question.normalizeImportRef(null));
        assertNull(Question.normalizeImportRef("   "));
    }

    @Test
    void referenceIsTrimmedAndKeptWhenTheShapeIsAllowed() {
        assertEquals("ai-7f3a.item_1-2", Question.normalizeImportRef("  ai-7f3a.item_1-2 "));
    }

    @Test
    void referenceWithSpacesOrSymbolsOrTooLongIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Question.normalizeImportRef("has space"));
        assertThrows(IllegalArgumentException.class, () -> Question.normalizeImportRef("a/b"));
        assertThrows(IllegalArgumentException.class, () -> Question.normalizeImportRef("x".repeat(101)));
    }

    @Test
    void createdQuestionCarriesItsReference() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, null, null, QuestionPurpose.LEARNING, "row-12");

        assertEquals("row-12", question.getImportRef());
    }
}
