package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningOverviewReader;
import com.ieltspath.learning.application.result.LearningOverviewStatistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class GetLearningOverviewUseCase {
    private final LearningOverviewReader reader;
    private final Clock clock;

    public GetLearningOverviewUseCase(LearningOverviewReader reader, Clock clock) {
        this.reader = reader;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LearningOverviewStatistics execute(UUID userId) {
        var totals = reader.totals(userId);
        return new LearningOverviewStatistics(clock.instant(), totals.dataFrom(), totals.studyMilliseconds() / 1000,
                totals.completedLessons());
    }
}
