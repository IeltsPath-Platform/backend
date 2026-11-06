package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.result.PracticeAttemptView;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PracticeAttemptViewAssemblerTest {
    private static final String SAMPLE = "Introduction.\n\nOverview.\n\nBody one.\n\nBody two.";

    private final WritingSubmissionRepository essays = mock(WritingSubmissionRepository.class);
    private final com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository speaking =
            mock(com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository.class);
    private final PracticeAttemptViewAssembler assembler = new PracticeAttemptViewAssembler(essays, speaking,
            SpeakingSettingsFixture.enabled());

    private PracticeAttemptView.Essay openEssay(String explanation) {
        Item essay = new Item(UUID.randomUUID(), 1, "Describe the chart.", null,
                Map.of("type", "ESSAY", "task", "TASK_1", "minWords", 150, "passBand", 5.5,
                        "chartFacts", "private chart figures"),
                explanation, "A hint.", BigDecimal.ONE, List.of());
        PackageVersion version = new PackageVersion(UUID.randomUUID(), UUID.randomUUID(), "PRACTICE_SET",
                UUID.randomUUID(), Map.of(),
                List.of(new Section(UUID.randomUUID(), "Writing", "WRITING", null, 1, null, List.of(essay))));
        PracticeAttempt attempt = PracticeAttempt.restore(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LearningSkill.WRITING, version.packageId(), version.packageVersionId(), Instant.now(), Instant.now(),
                UUID.randomUUID(), null);
        when(essays.latestForPractice(any(), any())).thenReturn(Map.of());
        return assembler.assemble(attempt, version).sections().getFirst().essays().getFirst();
    }

    @Test
    void anEssayOffersItsSampleAnswerBeforeAnySubmission() {
        PracticeAttemptView.Essay essay = openEssay(SAMPLE);

        assertThat(essay.sampleAnswer()).isEqualTo(SAMPLE);
        assertThat(essay.latestSubmission()).isNull();
    }

    @Test
    void theViewNeverCarriesTheAnswerSpec() {
        assertThat(openEssay(SAMPLE).toString()).doesNotContain("private chart figures");
    }

    @Test
    void anEssayWithoutAnExplanationHasNoSample() {
        assertThat(openEssay(null).sampleAnswer()).isNull();
    }

    private PracticeAttemptView recordedView(com.ieltspath.learning.domain.aggregate.SpeakingSubmission answer) {
        Item recorded = new Item(UUID.randomUUID(), 1, "Describe your weekend.", null,
                Map.of("type", "SPEAKING", "durationSeconds", 60, "passBand", 5.5, "private", "never shown"),
                "A tip.", "A hint.", BigDecimal.ONE, List.of());
        Item choice = new Item(UUID.randomUUID(), 2, "Pick one.", List.of(), Map.of("type", "CHOICE", "correct", "A"),
                null, null, BigDecimal.ONE, List.of());
        PackageVersion version = new PackageVersion(UUID.randomUUID(), UUID.randomUUID(), "PRACTICE_SET",
                UUID.randomUUID(), Map.of(),
                List.of(new Section(UUID.randomUUID(), "Speak", "SPEAKING", null, 1, null, List.of(recorded, choice))));
        PracticeAttempt attempt = PracticeAttempt.restore(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LearningSkill.SPEAKING, version.packageId(), version.packageVersionId(), Instant.now(), Instant.now(),
                UUID.randomUUID(), null);
        when(speaking.findLatestForAttempt(any(), any())).thenReturn(java.util.Optional.ofNullable(answer));
        return assembler.assemble(attempt, version);
    }

    @Test
    void aRecordedItemIsListedApartFromTheQuestionsWithoutItsAnswerSpec() {
        PracticeAttemptView view = recordedView(null);

        assertThat(view.questions()).hasSize(1);
        PracticeAttemptView.Recorded recorded = view.sections().getFirst().recorded().getFirst();
        assertThat(recorded.stem()).isEqualTo("Describe your weekend.");
        assertThat(recorded.durationSeconds()).isEqualTo(60);
        assertThat(recorded.passBand()).isEqualByComparingTo("5.5");
        assertThat(recorded.latestSubmission()).isNull();
        assertThat(view.toString()).doesNotContain("never shown");
    }

    @Test
    void theBandOfARecordedAnswerAppearsOnlyOnceItIsGraded() {
        var waiting = mock(com.ieltspath.learning.domain.aggregate.SpeakingSubmission.class);
        when(waiting.id()).thenReturn(UUID.randomUUID());
        when(waiting.status()).thenReturn(com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus.PAYMENT_PENDING);
        when(waiting.overallBand()).thenReturn(new BigDecimal("6.5"));
        assertThat(recordedView(waiting).sections().getFirst().recorded().getFirst().latestSubmission().overallBand()).isNull();

        var graded = mock(com.ieltspath.learning.domain.aggregate.SpeakingSubmission.class);
        when(graded.id()).thenReturn(UUID.randomUUID());
        when(graded.status()).thenReturn(com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus.GRADED);
        when(graded.overallBand()).thenReturn(new BigDecimal("5.0"));
        var latest = recordedView(graded).sections().getFirst().recorded().getFirst().latestSubmission();
        assertThat(latest.overallBand()).isEqualByComparingTo("5.0");
        assertThat(latest.passed()).isFalse();
    }
}
