package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.PracticeAttemptReader;
import com.ieltspath.assessment.application.result.PracticeProgressResult;
import com.ieltspath.assessment.domain.vo.AttemptScope;
import com.ieltspath.assessment.domain.vo.Skill;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetPracticeProgressUseCase {
    private final PracticeAttemptReader reader;

    public GetPracticeProgressUseCase(PracticeAttemptReader reader) { this.reader = reader; }

    private static final int MAX_PACKAGE_IDS = 200;

    /** An empty {@code packageIds} returns the progress of every package, otherwise only of the listed ones. */
    public PracticeProgressResult execute(UUID userId, Skill skill, AttemptScope scope, List<UUID> packageIds) {
        if (packageIds.size() > MAX_PACKAGE_IDS) throw new IllegalArgumentException("Too many package ids");
        return new PracticeProgressResult(reader.findProgress(userId, skill, scope, packageIds).stream()
                .map(key -> new PracticeProgressResult.Key(key.packageId(), key.partNo())).toList());
    }
}
