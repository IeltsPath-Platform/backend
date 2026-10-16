package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.PracticeAttemptReader;
import com.ieltspath.assessment.application.result.PracticeHistoryResult;
import com.ieltspath.assessment.domain.vo.Skill;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetPracticeHistoryUseCase {
    private final PracticeAttemptReader reader;

    public GetPracticeHistoryUseCase(PracticeAttemptReader reader) { this.reader = reader; }

    public PracticeHistoryResult execute(UUID userId, Skill skill, int page, int size) {
        if (page < 0 || size < 1 || size > 50) throw new IllegalArgumentException("Invalid history page or size");
        var result = reader.findHistory(userId, skill, page, size);
        return new PracticeHistoryResult(result.totalElements(), page, size, result.items().stream()
                .map(item -> new PracticeHistoryResult.Item(item.attemptId(), item.packageId(), item.packageVersionId(),
                        item.skill(), item.scope(), item.partNos(), item.status(), item.submittedAt(), item.percent())).toList());
    }
}
