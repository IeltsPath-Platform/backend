package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.ErrorTrapResult;
import com.ieltspath.learning.application.result.SubmissionResult;
import com.ieltspath.learning.application.result.ReviewSubmissionResult;
import com.ieltspath.learning.application.result.DiagnosticProbeSubmissionResult;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorTrapFeedbackResponseTest {
    private static final ErrorTrapResult ERROR = new ErrorTrapResult("TFNG_NG_AS_FALSE", "Not Given as False",
            "Check whether the passage states this before choosing False.");

    @Test
    void submissionResponseKeepsFeedbackForUnpassedAndPassedBlocks() {
        UUID questionId = UUID.randomUUID();
        var unpassed = SubmissionResponse.from(new SubmissionResult(false, false,
                List.of(new SubmissionResult.AnswerResult(questionId, false, "NOT_GIVEN", "Explanation", null,
                        ERROR))));
        var passed = SubmissionResponse.from(new SubmissionResult(true, true,
                List.of(new SubmissionResult.AnswerResult(questionId, false, "NOT_GIVEN", "Explanation", null,
                        ERROR))));

        assertThat(((SubmissionResponse.Correctness) unpassed.results().getFirst()).error()).isEqualTo(ERROR);
        assertThat(((SubmissionResponse.SolvedAnswer) passed.results().getFirst()).error()).isEqualTo(ERROR);
    }

    @Test
    void practiceSubmissionResponseKeepsFeedback() {
        var result = new PracticeSubmission(UUID.randomUUID(), 0, 1, 0, false, true,
                List.of(new PracticeSubmission.Answer(UUID.randomUUID(), false, "NOT_GIVEN", "Explanation", null,
                        new PracticeSubmission.ErrorTrap(ERROR.code(), ERROR.name(), ERROR.fixHint()))),
                null, List.of(), List.of(), null);

        assertThat(PracticeSubmissionResponse.from(result).results().getFirst().error()).isEqualTo(ERROR);
    }

    @Test
    void reviewAndDiagnosticResponsesKeepFeedback() {
        var answer = new SubmissionResult.AnswerResult(UUID.randomUUID(), false, "NOT_GIVEN", "Explanation", null,
                ERROR);
        var review = ReviewSubmissionResponse.from(new ReviewSubmissionResult("DONE", true, List.of(answer), null));
        var diagnostic = DiagnosticProbeResponse.Submission.from(new DiagnosticProbeSubmissionResult(
                UUID.randomUUID(), false, 0, 1, List.of(answer), null, "CONCLUDED", List.of()));

        assertThat(((SubmissionResponse.SolvedAnswer) review.results().getFirst()).error()).isEqualTo(ERROR);
        assertThat(diagnostic.results().getFirst().error()).isEqualTo(ERROR);
    }
}
