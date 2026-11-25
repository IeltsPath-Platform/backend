package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.service.MasteryCalculator;
import com.ieltspath.learning.domain.vo.MasteryHistory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The mastery a learner reads (SRS FT-26). */
class GetMasteryUseCaseTest {
    private final KnowledgeEvidenceRepository evidence = mock(KnowledgeEvidenceRepository.class);
    private final GetMasteryUseCase useCase = new GetMasteryUseCase(evidence);
    private final UUID user = UUID.randomUUID();

    @Test
    void aPointWithoutEvidenceHasMasteryZeroAndNoEvidence() {
        UUID point = UUID.randomUUID();
        when(evidence.findMasteryHistories(user)).thenReturn(List.of(new MasteryHistory(point, UUID.randomUUID(), false, List.of(), 0)));

        var result = useCase.execute(user);

        assertThat(result).singleElement().satisfies(found -> {
            assertThat(found.knowledgePointId()).isEqualTo(point);
            assertThat(found.mastery()).isZero();
            assertThat(found.evidenceCount()).isZero();
        });
    }

    @Test
    void sixOutcomesUseOnlyTheLatestFiveAndCountSix() {
        UUID point = UUID.randomUUID();
        List<Boolean> six = List.of(false, true, true, true, true, true);
        when(evidence.findMasteryHistories(user)).thenReturn(List.of(new MasteryHistory(point, UUID.randomUUID(), false, six, 6)));

        var result = useCase.execute(user).getFirst();

        double latestFive = new MasteryCalculator().compute(List.of(true, true, true, true, true));
        assertThat(result.mastery()).isEqualTo(latestFive);
        assertThat(result.evidenceCount()).isEqualTo(6);
    }
}
