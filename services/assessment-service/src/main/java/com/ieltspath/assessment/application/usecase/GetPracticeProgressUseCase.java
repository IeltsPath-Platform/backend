package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.PracticeAttemptReader;
import com.ieltspath.assessment.application.result.PracticeProgressResult;
import com.ieltspath.assessment.domain.vo.AttemptScope;
import com.ieltspath.assessment.domain.vo.Skill;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetPracticeProgressUseCase {
    private final PracticeAttemptReader reader;

    public GetPracticeProgressUseCase(PracticeAttemptReader reader) { this.reader = reader; }

    public PracticeProgressResult execute(UUID userId, Skill skill, AttemptScope scope) {
        return new PracticeProgressResult(reader.findProgress(userId, skill, scope, 500).stream()
                .map(key -> new PracticeProgressResult.Key(key.packageId(), key.partNo())).toList());
    }
}
