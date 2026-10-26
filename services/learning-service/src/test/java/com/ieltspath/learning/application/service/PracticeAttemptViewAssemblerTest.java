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
    private final PracticeAttemptViewAssembler assembler = new PracticeAttemptViewAssembler(essays);

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
}
