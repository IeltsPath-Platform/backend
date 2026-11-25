package com.ieltspath.content.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.domain.vo.QuestionType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SectionQuestionChecksTest {
    private final ObjectMapper json = new ObjectMapper();
    private String check(String candidate, QuestionType type, String source) throws Exception {
        return SectionQuestionChecks.reason(json.readTree(candidate), type, source, List.of(), List.of(), .8, .5, 4, 12, 400);
    }
    @Test void rejectsAnAnswerThatAppearsOnlyInsideAnotherWord() throws Exception {
        assertEquals("ANSWER_NOT_IN_SOURCE", check("""
                {"stem":"Which word? ______","explanation":"Evidence","accepted":["art"]}
                """, QuestionType.FILL_IN_BLANK, "A party welcomes guests."));
    }
    @Test void acceptsAWordBoundaryAnswerAndRequiresExactlyOneBlank() throws Exception {
        assertNull(check("""
                {"stem":"Which course? ______","explanation":"Evidence","accepted":["cooking"]}
                """, QuestionType.FILL_IN_BLANK, "The cooking course starts in June and runs for six weeks."));
    }
    @Test void rejectsAnswersOutsideTheSourceAndMultipleBlanks() throws Exception {
        assertEquals("BAD_BLANK", check("""
                {"stem":"Which course? ______ ______","explanation":"Evidence","accepted":["cooking"]}
                """, QuestionType.FILL_IN_BLANK, "The cooking course starts in June and runs for six weeks."));
    }

    @Test void rejectsAnAnswerInTheStemAndAnswersThatAreTooLong() throws Exception {
        assertEquals("ANSWER_IN_STEM", check("""
                {"stem":"What is cooking? ______","explanation":"Evidence","accepted":["cooking"]}
                """, QuestionType.FILL_IN_BLANK, "The cooking course starts in June."));
        assertEquals("ANSWER_TOO_LONG", check("""
                {"stem":"Which description? ______","explanation":"Evidence","accepted":["the small garden beside the old library is open every day for visitors"]}
                """, QuestionType.FILL_IN_BLANK, "The small garden beside the old library is open every day for visitors."));
    }

    @Test void rejectsInvalidTfngAndDuplicateMultipleChoiceOptions() throws Exception {
        assertEquals("BAD_TFNG_OPTIONS", check("""
                {"stem":"Does the notice mention a time?","explanation":"Evidence","options":[
                  {"optionKey":"A","content":"True"},{"optionKey":"B","content":"False"},{"optionKey":"C","content":"Not given"}],"correct":"A"}
                """, QuestionType.TRUE_FALSE_NOT_GIVEN, "The notice mentions a time."));
        assertEquals("DUPLICATE_OPTIONS", check("""
                {"stem":"Which service is available?","explanation":"Evidence","options":[
                  {"optionKey":"A","content":"Free parking"},{"optionKey":"B","content":"free parking"}],"correct":"A"}
                """, QuestionType.MULTIPLE_CHOICE, "Parking is available."));
    }

    @Test void rejectsLongCopiedTextAndDuplicatesAgainstExistingOrKeptQuestions() throws Exception {
        String copied = "The small garden beside the old library is open every day for visitors to enjoy quietly";
        assertEquals("COPIED_FROM_SOURCE", check("""
                {"stem":"The small garden beside the old library is open every day for visitors to enjoy quietly?","explanation":"Evidence","options":[
                  {"optionKey":"A","content":"Yes"},{"optionKey":"B","content":"No"}],"correct":"A"}
                """, QuestionType.MULTIPLE_CHOICE, copied + " during spring."));

        var candidate = json.readTree("""
                {"stem":"Which service is available to visitors?","explanation":"Evidence","options":[
                  {"optionKey":"A","content":"Free parking"},{"optionKey":"B","content":"A cafe"}],"correct":"A"}
                """);
        assertEquals("DUPLICATE_IN_SECTION:1", SectionQuestionChecks.reason(candidate, QuestionType.MULTIPLE_CHOICE,
                "Visitors can use the service.", List.of("Which service is available to visitors?"), List.of(), .8, .5, 4, 12, 400));
        assertEquals("DUPLICATE_IN_DRAFT", SectionQuestionChecks.reason(candidate, QuestionType.MULTIPLE_CHOICE,
                "Visitors can use the service.", List.of(), List.of("Which service is available to visitors?"), .8, .5, 4, 12, 400));
    }
}
