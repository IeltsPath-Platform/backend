package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.api.dto.response.PracticeSubmissionResponse;
import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SubmittedAnswersTest {
    private static SubmitExerciseCommand.Answer answer(UUID question, Object value) {
        return new SubmitExerciseCommand.Answer(question, value, null);
    }

    @Test
    void keepsWhatWasWrittenForEachQuestionAndSkipsTheUnanswered() {
        UUID first = UUID.randomUUID();
        UUID blank = UUID.randomUUID();
        UUID missing = UUID.randomUUID();

        Map<UUID, String> submitted = SubmittedAnswers.of(List.of(answer(first, "  report "), answer(blank, "   "), answer(missing, null)));

        assertEquals(Map.of(first, "report"), submitted);
    }

    @Test
    void cutsALongAnswerBecauseTheGradeIsStoredWithIt() {
        UUID question = UUID.randomUUID();

        String stored = SubmittedAnswers.of(List.of(answer(question, "x".repeat(2000)))).get(question);

        assertEquals(SubmittedAnswers.MAX_LENGTH, stored.length());
    }

    @Test
    void noAnswersGivesNothing() {
        assertEquals(Map.of(), SubmittedAnswers.of(null));
    }

    @Test
    void aGradeStoredBeforeAnswersWereKeptStillReadsAndHasNoSubmittedAnswer() throws Exception {
        UUID attempt = UUID.randomUUID();
        UUID question = UUID.randomUUID();
        String stored = """
                {"attemptId":"%s","correct":1,"total":1,"percent":1.0,"passed":true,"countedAsEvidence":true,
                 "results":[{"questionVersionId":"%s","correct":true,"correctAnswer":"B","explanation":"Because.","hint":null,"error":null}],
                 "transcript":null,"reviewsCreated":[],"skillScores":[],"gamification":null}
                """.formatted(attempt, question);

        PracticeSubmission read = new ObjectMapper().readValue(stored, PracticeSubmission.class);

        assertNull(read.results().getFirst().submitted());
        assertFalse(new ObjectMapper().writeValueAsString(PracticeSubmissionResponse.from(read)).contains("submittedAnswer"));
    }

    @Test
    void theSubmittedAnswerSurvivesStorageAndReachesTheResponse() throws Exception {
        UUID question = UUID.randomUUID();
        var saved = new PracticeSubmission(UUID.randomUUID(), 0, 1, 0, false, true,
                List.of(new PracticeSubmission.Answer(question, false, "report", "Because.", null, null, "describe")), null, List.of());
        ObjectMapper json = new ObjectMapper();

        PracticeSubmission read = json.readValue(json.writeValueAsString(saved), PracticeSubmission.class);

        assertEquals("describe", read.results().getFirst().submitted());
        assertEquals("describe", PracticeSubmissionResponse.from(read).results().getFirst().submittedAnswer());
    }
}
