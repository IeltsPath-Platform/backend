package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.WritingGrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.ieltspath.learning.domain.vo.WritingSubmissionStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WritingSubmissionTest {
    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");
    private static final EssayPrompt PROMPT = new EssayPrompt(UUID.randomUUID(), "Stem", "TASK_2", 250,
            new BigDecimal("6"), null, "Sample", List.of(), List.of(UUID.randomUUID()));

    private static WritingSubmission grading() {
        return WritingSubmission.start(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "essay", 260, PROMPT, 3, NOW);
    }

    private static WritingGrade band(String overall) {
        return new WritingGrade(List.of(), List.of(), "summary", new BigDecimal(overall));
    }

    @Test
    void gradedThenPaidBecomesGradedWithPassAtThePassBand() {
        WritingSubmission submission = grading();
        assertThat(submission.isNew()).isTrue();

        submission.recordGrade(band("6.0"));
        assertThat(submission.status()).isEqualTo(PAYMENT_PENDING);
        assertThat(submission.passed()).isTrue();

        submission.recordPaymentFailure("INSUFFICIENT_POINTS");
        assertThat(submission.status()).isEqualTo(PAYMENT_PENDING);
        assertThat(submission.failureCode()).isEqualTo("INSUFFICIENT_POINTS");

        UUID ledger = UUID.randomUUID();
        submission.markGraded(ledger);
        assertThat(submission.status()).isEqualTo(GRADED);
        assertThat(submission.ledgerEntryId()).isEqualTo(ledger);
        assertThat(submission.failureCode()).isNull();
    }

    @Test
    void belowThePassBandDoesNotPass() {
        WritingSubmission submission = grading();
        submission.recordGrade(band("5.5"));
        assertThat(submission.passed()).isFalse();
    }

    @Test
    void failedGradingCanRestartOnlyFromFailed() {
        WritingSubmission submission = grading();
        submission.fail("LLM_UNAVAILABLE");
        assertThat(submission.status()).isEqualTo(FAILED);

        Instant later = NOW.plusSeconds(30);
        submission.restart(later);
        assertThat(submission.status()).isEqualTo(GRADING);
        assertThat(submission.failureCode()).isNull();
        assertThat(submission.gradingStartedAt()).isEqualTo(later);

        assertThatThrownBy(() -> submission.restart(later)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void staleGradingIsAbandoned() {
        WritingSubmission submission = grading();
        assertThat(submission.isStale(NOW)).isFalse();
        assertThat(submission.isStale(NOW.plusSeconds(1))).isTrue();

        submission.abandon();
        assertThat(submission.status()).isEqualTo(FAILED);
        assertThat(submission.failureCode()).isEqualTo(WritingSubmission.ABANDONED);
        assertThat(submission.isStale(NOW.plusSeconds(1))).isFalse();
    }

    @Test
    void transitionsOutOfOrderAreRejected() {
        WritingSubmission submission = grading();
        assertThatThrownBy(() -> submission.markGraded(UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> submission.recordPaymentFailure("X")).isInstanceOf(IllegalStateException.class);

        submission.recordGrade(band("7.0"));
        assertThatThrownBy(() -> submission.fail("LLM_UNAVAILABLE")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> submission.recordGrade(band("7.0"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void persistedStatusFollowsSuccessfulWrites() {
        WritingSubmission submission = WritingSubmission.restore(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "essay", 260, PROMPT, 3, GRADING, null, null,
                null, null, NOW);
        submission.recordGrade(band("6.5"));
        assertThat(submission.persistedStatus()).isEqualTo(GRADING);

        submission.persisted();
        assertThat(submission.persistedStatus()).isEqualTo(PAYMENT_PENDING);
    }
}
