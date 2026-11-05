package com.ieltspath.learning.application.result;

import com.ieltspath.learning.application.result.ErrorTrapResult;
import com.ieltspath.learning.application.result.SubmissionResult.AnswerResult;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorTrapFeedbackContractTest {
    private final ErrorTrapResult error = new ErrorTrapResult("SPELLING", "Spelling", "Check the ending.");

    @Test
    void lessonReviewAndDiagnosticAnswerResultsCarryOptionalErrorText() {
        AnswerResult result = new AnswerResult(UUID.randomUUID(), false, "went", "Explanation", null, error);
        assertThat(result.error()).isEqualTo(error);
    }

    @Test
    void practiceAndFreePracticeAnswerResultsCarryOptionalErrorText() {
        PracticeSubmission.ErrorTrap trap = new PracticeSubmission.ErrorTrap(error.code(), error.name(), error.fixHint());
        PracticeSubmission.Answer result = new PracticeSubmission.Answer(UUID.randomUUID(), false,
                "went", "Explanation", null, trap);
        assertThat(result.error()).isEqualTo(trap);
    }
}
