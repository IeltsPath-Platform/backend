package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SpeakingPathUpdaterTest {
    private final KnowledgeEvidenceRepository evidence = mock(KnowledgeEvidenceRepository.class);
    private final MasteryProfileUpdater mastery = mock(MasteryProfileUpdater.class);
    private final SpeakingPathUpdater updater = new SpeakingPathUpdater(evidence, mastery);
    private final UUID user = UUID.randomUUID();
    private final UUID submission = UUID.randomUUID();
    private static final BigDecimal PASS = new BigDecimal("6.0");

    @Test
    void writesOneEvidenceRowPerMappedCriterionFromItsBand() {
        updater.apply(user, submission, SpeakingTestData.rubric(), Map.of("usableAsEvidence", true, "criteria", List.of(
                Map.of("code", "FC", "band", new BigDecimal("6.5")),
                Map.of("code", "LR", "band", new BigDecimal("4.0")),            // no knowledge point mapped: skipped
                Map.of("code", "PR", "band", new BigDecimal("5.5")))), PASS);

        @SuppressWarnings("unchecked") ArgumentCaptor<List<KnowledgeEvidence>> rows = ArgumentCaptor.forClass(List.class);
        verify(evidence).append(org.mockito.ArgumentMatchers.eq(user), rows.capture());
        assertThat(rows.getValue()).extracting(KnowledgeEvidence::knowledgePointId)
                .containsExactly(SpeakingTestData.FC_KP, SpeakingTestData.PR_KP);
        assertThat(rows.getValue()).extracting(KnowledgeEvidence::correct).containsExactly(true, false);
        assertThat(rows.getValue()).allSatisfy(row -> assertThat(row.source()).isEqualTo(EvidenceSource.SPEAKING_PRACTICE));
        verify(mastery).recordEvidence(org.mockito.ArgumentMatchers.eq(user), any());
    }

    @Test
    void doesNothingWhenTheTranscriptWasNotReliableEnough() {
        updater.apply(user, submission, SpeakingTestData.rubric(), Map.of("usableAsEvidence", false,
                "criteria", List.of(Map.of("code", "FC", "band", new BigDecimal("6.5")))), PASS);

        verify(evidence, never()).append(any(), any());
        verify(mastery, never()).recordEvidence(any(), any());
    }

    @Test
    void doesNothingWithoutSignals() {
        updater.apply(user, submission, SpeakingTestData.rubric(), null, PASS);

        verify(evidence, never()).append(any(), any());
    }
}
