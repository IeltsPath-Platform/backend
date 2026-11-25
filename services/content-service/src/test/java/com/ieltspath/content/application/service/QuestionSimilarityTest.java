package com.ieltspath.content.application.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuestionSimilarityTest {
    @Test void punctuationAndDiacriticsNormalizeBeforeSimilarity() {
        assertEquals(1.0, QuestionSimilarity.jaccard("Thời gian: ______ cooking!", "Thoi gian ______ cooking"));
    }
    @Test void shortQuestionsKeepFunctionWords() {
        assertEquals(1.0, QuestionSimilarity.jaccard("What is it?", "What is it"));
    }
    @Test void answerSubstringsDoNotMatchAnotherWord() {
        assertFalse((" party ").contains(" art "));
    }
    @Test void hyphensBecomeWordBoundaries() {
        assertEquals(1.0, QuestionSimilarity.jaccard("day-of-class", "day of class"));
    }
    @Test void spelledNumbersMatchTheirDigitForms() {
        assertEquals(QuestionSimilarity.normalize("twenty one days"), QuestionSimilarity.normalize("21 days"));
        assertEquals(QuestionSimilarity.normalize("one hundred"), QuestionSimilarity.normalize("100"));
    }
}
