package com.ieltspath.content.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.domain.vo.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuestionDraftValidatorTest {

    private static final Set<String> CODES = Set.of("PARAPHRASE_TRAP");
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void wellFormedChoiceQuestionPasses() throws Exception {
        assertNull(reason(choice("\"correct\":\"A\",\"distractors\":{\"B\":\"PARAPHRASE_TRAP\"}"),
                QuestionType.MULTIPLE_CHOICE));
    }

    @Test
    void correctKeyMustBeAnOption() throws Exception {
        assertEquals("CORRECT_NOT_AN_OPTION", reason(choice("\"correct\":\"Z\""), QuestionType.MULTIPLE_CHOICE));
    }

    @Test
    void duplicateOptionKeysAreRefused() throws Exception {
        JsonNode item = json.readTree("{\"stem\":\"S\",\"explanation\":\"E\",\"correct\":\"A\",\"options\":["
                + "{\"optionKey\":\"A\",\"content\":\"x\"},{\"optionKey\":\"A\",\"content\":\"y\"}]}");
        assertEquals("BAD_OPTIONS", reason(item, QuestionType.MULTIPLE_CHOICE));
    }

    @Test
    void distractorMustPointAtAWrongOptionAndAnActiveCode() throws Exception {
        assertEquals("DISTRACTOR_NOT_A_WRONG_OPTION",
                reason(choice("\"correct\":\"A\",\"distractors\":{\"A\":\"PARAPHRASE_TRAP\"}"), QuestionType.MULTIPLE_CHOICE));
        assertEquals("UNKNOWN_ERROR_CODE",
                reason(choice("\"correct\":\"A\",\"distractors\":{\"B\":\"MADE_UP\"}"), QuestionType.MULTIPLE_CHOICE));
    }

    @Test
    void typedAnswerNeedsAnAcceptedValue() throws Exception {
        assertNull(reason(json.readTree("{\"stem\":\"S\",\"explanation\":\"E\",\"accepted\":[\"critics\"]}"),
                QuestionType.FILL_IN_BLANK));
        assertEquals("NO_ACCEPTED_ANSWER", reason(json.readTree("{\"stem\":\"S\",\"explanation\":\"E\",\"accepted\":[]}"),
                QuestionType.FILL_IN_BLANK));
    }

    @Test
    void missingExplanationIsRefused() throws Exception {
        assertEquals("NO_EXPLANATION",
                reason(json.readTree("{\"stem\":\"S\",\"accepted\":[\"x\"]}"), QuestionType.SHORT_ANSWER));
    }

    @Test
    void aStemTheKnowledgePointAlreadyHasIsADuplicateWhateverItsLayout() throws Exception {
        JsonNode item = json.readTree("{\"stem\":\"  What   IS the\\nanswer? \",\"explanation\":\"E\","
                + "\"accepted\":[\"x\"]}");

        assertEquals("DUPLICATE_STEM", QuestionDraftValidator.rejectionReason(item, QuestionType.SHORT_ANSWER, CODES,
                Set.of("what is the answer?")));
        assertNull(QuestionDraftValidator.rejectionReason(item, QuestionType.SHORT_ANSWER, CODES,
                Set.of("what is the question?")));
    }

    private String reason(JsonNode item, QuestionType type) {
        return QuestionDraftValidator.rejectionReason(item, type, CODES, Set.of());
    }

    /** A choice item with a valid stem, explanation and two options, plus the given fields. */
    private JsonNode choice(String fields) throws Exception {
        return json.readTree("{\"stem\":\"S\",\"explanation\":\"E\",\"options\":["
                + "{\"optionKey\":\"A\",\"content\":\"x\"},{\"optionKey\":\"B\",\"content\":\"y\"}]," + fields + "}");
    }
}
