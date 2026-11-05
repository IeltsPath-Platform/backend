package com.ieltspath.content.application.service;

import com.ieltspath.content.application.service.LessonSummaryComposer.Word;
import com.ieltspath.content.domain.vo.LessonSummaryMarkdown;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonSummaryComposerTest {

    private static final String TABLE = "## Vocabulary Core\n\n| Từ | Nghĩa |\n| --- | --- |\n";

    @Test
    void appendsTheWordsAsATableAfterTheSummary() {
        String result = LessonSummaryComposer.withVocabularyCore("## Key Points\n\n- one",
                List.of(new Word("topic", "chủ đề"), new Word("infer", "suy luận")));

        assertEquals("## Key Points\n\n- one\n\n" + TABLE + "| topic | chủ đề |\n| infer | suy luận |", result);
    }

    @Test
    void aLessonWithoutASummaryGetsJustTheTable() {
        assertEquals(TABLE + "| topic | chủ đề |", LessonSummaryComposer.withVocabularyCore(null, List.of(new Word("topic", "chủ đề"))));
        assertEquals(TABLE + "| topic | chủ đề |", LessonSummaryComposer.withVocabularyCore("  ", List.of(new Word("topic", "chủ đề"))));
    }

    @Test
    void aLessonWithoutWordsKeepsItsSummaryAsItIs() {
        assertEquals("- one", LessonSummaryComposer.withVocabularyCore("- one", List.of()));
        assertNull(LessonSummaryComposer.withVocabularyCore(null, List.of(new Word(" ", "x"))));
    }

    @Test
    void aSummaryThatAlreadyHasTheSectionIsNotChanged() {
        String own = "## Vocabulary Core\n\n| Từ | Nghĩa |\n| --- | --- |\n| mine | của tôi |";

        assertEquals(own, LessonSummaryComposer.withVocabularyCore(own, List.of(new Word("topic", "chủ đề"))));
    }

    @Test
    void aWordIsListedOnceWhateverItsCaseAndKeepsTheFirstMeaningThatWasGiven() {
        String result = LessonSummaryComposer.withVocabularyCore(null,
                List.of(new Word("Topic", ""), new Word("topic", "chủ đề"), new Word("TOPIC", "khác")));

        assertEquals(TABLE + "| Topic | chủ đề |", result);
    }

    @Test
    void aMissingMeaningIsADashAndASeparatorInTextCannotBreakTheTable() {
        String result = LessonSummaryComposer.withVocabularyCore(null,
                List.of(new Word("a|b", null), new Word("two\nlines", "x | y")));

        assertEquals(TABLE + "| a\\|b | — |\n| two lines | x \\| y |", result);
    }

    @Test
    void aSummaryTooLongToTakeTheTableIsLeftAlone() {
        String full = "x".repeat(LessonSummaryMarkdown.MAX_LENGTH - 5);

        String result = LessonSummaryComposer.withVocabularyCore(full, List.of(new Word("topic", "chủ đề")));

        assertEquals(full, result);
        assertTrue(result.length() <= LessonSummaryMarkdown.MAX_LENGTH);
    }
}
