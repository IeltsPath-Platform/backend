package com.ieltspath.assessment.application.port;

import java.util.UUID;
import java.util.Collection;

public interface EssayGradeComparisonStore {
    /** Records the examiner half without overwriting an earlier or later LLM grade. */
    void recordExaminer(UUID attemptId, UUID resultId, UUID examinerId, Collection<UUID> assignedSubmissions);
}
