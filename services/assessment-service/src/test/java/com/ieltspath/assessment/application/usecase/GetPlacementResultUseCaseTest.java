package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.result.PlacementResult;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AssessmentResult;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GetPlacementResultUseCaseTest {
    @Test
    void returnsStoredPoolCeilingAndPartialMetadata() {
        UUID user = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        Instant now = Instant.now();
        var attempt = new AssessmentAttempt(id, user, UUID.randomUUID(), AttemptType.PLACEMENT, AttemptMode.STANDARD,
                AttemptChannel.WEB, AttemptStatus.SUBMITTED, now, now, null, 1, now, now, null, null,
                AttemptScope.FULL, null, PlacementPool.BAND_0_3);
        AssessmentAttemptRepository attempts = mock(AssessmentAttemptRepository.class);
        AssessmentResultRepository results = mock(AssessmentResultRepository.class);
        PlacementGradingService grading = mock(PlacementGradingService.class);
        PlacementReportAssembler report = mock(PlacementReportAssembler.class);
        when(attempts.findByIdAndUserId(id, user)).thenReturn(Optional.of(attempt));
        when(results.findLatestCompletedByAttemptId(id)).thenReturn(Optional.of(new AssessmentResult(resultId, id, 1,
                AssessmentResult.COMPLETED, 3.0, now, PlacementPool.BAND_0_3, 6.5, true)));
        when(grading.skillBands(id, resultId)).thenReturn(java.util.Map.of(Skill.LISTENING, 6.5));
        when(report.sections(id, resultId)).thenReturn(List.of());

        PlacementResult result = new GetPlacementResultUseCase(attempts, results, grading, report).execute(user, id);

        assertThat(result.pool()).isEqualTo(PlacementPool.BAND_0_3);
        assertThat(result.ceiling()).isEqualTo(3.0);
        assertThat(result.reachedCeiling()).isTrue();
        assertThat(result.partial()).isTrue();
    }
}
