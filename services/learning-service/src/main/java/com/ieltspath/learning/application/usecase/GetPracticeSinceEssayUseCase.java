package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository;
import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository.PracticeTally;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What the learner practised on the knowledge points an essay's result suggests, since that essay was submitted: for each
 * point, how many free practice sets and the latest one. Only the owner of the essay asks; another learner's essay is not found.
 * A point not practised since is left out.
 */
@Service
public class GetPracticeSinceEssayUseCase {
    public static final int MAX_POINTS = 20;

    private final SubmissionHistoryRepository history;

    public GetPracticeSinceEssayUseCase(SubmissionHistoryRepository history) {
        this.history = history;
    }

    public List<PracticeTally> execute(UUID userId, UUID submissionId, List<UUID> knowledgePointIds) {
        Set<UUID> points = new LinkedHashSet<>(knowledgePointIds == null ? List.of() : knowledgePointIds);
        points.remove(null);
        if (points.size() > MAX_POINTS) {
            throw new IllegalArgumentException("Chỉ được chọn tối đa " + MAX_POINTS + " điểm kiến thức.");
        }
        Instant since = history.essaySubmittedAt(userId, submissionId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy bài nộp."));
        return points.isEmpty() ? List.of() : history.tallyKnowledgePractice(userId, points, since);
    }
}
