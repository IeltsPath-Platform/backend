package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.PracticeProgressResult;
import com.ieltspath.learning.domain.repository.PracticeProgressRepository;
import com.ieltspath.learning.domain.vo.PracticeProgressSkill;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Which practice tests of the catalog the learner has finished in Speaking and Writing, so the catalog can list them under
 * "done". Only graded answers count. The answers are kept by package version or by question; the catalog knows a test by
 * its package and part, so Content translates them. This reads first and asks Content afterwards, with no transaction
 * held while the call is out.
 */
@Service
public class GetPracticeProgressUseCase {
    private final PracticeProgressRepository progress;
    private final LearningContentClient content;

    public GetPracticeProgressUseCase(PracticeProgressRepository progress, LearningContentClient content) {
        this.progress = progress;
        this.content = content;
    }

    public PracticeProgressResult execute(UUID userId, PracticeProgressSkill skill, SpeakingScope scope) {
        return switch (skill) {
            case SPEAKING -> speaking(userId, scope);
            // A Writing test is always one task, so there is no full Writing test to have finished.
            case WRITING -> scope == SpeakingScope.FULL ? new PracticeProgressResult(List.of()) : writing(userId);
        };
    }

    private PracticeProgressResult speaking(UUID userId, SpeakingScope scope) {
        List<PracticeProgressRepository.GradedSpeaking> graded = progress.gradedSpeaking(userId, scope);
        if (graded.isEmpty()) return new PracticeProgressResult(List.of());
        LearningContentClient.PracticeTestKeys keys = content.practiceTestKeys(
                graded.stream().map(PracticeProgressRepository.GradedSpeaking::packageVersionId).distinct().toList(), List.of());
        Latest latest = new Latest();
        for (PracticeProgressRepository.GradedSpeaking item : graded) {
            UUID packageId = keys.packageByVersion().get(item.packageVersionId());
            if (packageId != null) latest.offer(packageId, scope == SpeakingScope.PART ? item.partNo() : null, item.submissionId(), item.doneAt());
        }
        return latest.result();
    }

    private PracticeProgressResult writing(UUID userId) {
        List<PracticeProgressRepository.GradedWriting> graded = progress.gradedFreeWriting(userId);
        if (graded.isEmpty()) return new PracticeProgressResult(List.of());
        LearningContentClient.PracticeTestKeys keys = content.practiceTestKeys(List.of(),
                graded.stream().map(PracticeProgressRepository.GradedWriting::questionVersionId).distinct().toList());
        Latest latest = new Latest();
        for (PracticeProgressRepository.GradedWriting item : graded) {
            LearningContentClient.PracticeTestQuestionKey key = keys.byQuestion().get(item.questionVersionId());
            if (key != null) latest.offer(key.packageId(), key.partNo(), item.submissionId(), item.doneAt());
        }
        return latest.result();
    }

    /**
     * One entry per test: an older version of a package and a newer one are the same test, so the most recent graded
     * answer is the one to open.
     */
    private static final class Latest {
        private final Map<String, PracticeProgressResult.Key> keys = new LinkedHashMap<>();
        private final Map<String, Instant> doneAt = new LinkedHashMap<>();

        void offer(UUID packageId, Integer partNo, UUID submissionId, Instant at) {
            String id = packageId + ":" + (partNo == null ? "full" : partNo);
            Instant seen = doneAt.get(id);
            if (seen != null && !at.isAfter(seen)) return;
            doneAt.put(id, at);
            keys.put(id, new PracticeProgressResult.Key(packageId, partNo, submissionId));
        }

        PracticeProgressResult result() {
            return new PracticeProgressResult(new ArrayList<>(keys.values()));
        }
    }
}
