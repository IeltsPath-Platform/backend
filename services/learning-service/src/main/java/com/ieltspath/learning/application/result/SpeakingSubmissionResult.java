package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.vo.SpeakingRecording;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A Speaking submission as the learner sees it: its state and which questions already have a recording. */
public record SpeakingSubmissionResult(UUID id, UUID packageVersionId, SpeakingScope scope,
                                       SpeakingSubmissionStatus status, int pointCost, String failureCode,
                                       Instant createdAt, List<Recording> recordings, Map<String, Object> result) {

    public SpeakingSubmissionResult(UUID id, UUID packageVersionId, SpeakingScope scope,
                                    SpeakingSubmissionStatus status, int pointCost, String failureCode,
                                    Instant createdAt, List<Recording> recordings) {
        this(id, packageVersionId, scope, status, pointCost, failureCode, createdAt, recordings, null);
    }

    /** Object keys stay on the server: the learner learns only whether a question has its file. */
    public record Recording(UUID questionVersionId, UUID sectionId, int partNo, int questionNo, boolean uploaded,
                            Integer durationMs) {
        static Recording from(SpeakingRecording recording) {
            return new Recording(recording.questionVersionId(), recording.sectionId(), recording.partNo(),
                    recording.questionNo(), recording.isUploaded(), recording.durationMs());
        }
    }

    public static SpeakingSubmissionResult from(SpeakingSubmission submission) {
        return new SpeakingSubmissionResult(submission.id(), submission.packageVersionId(), submission.scope(),
                submission.status(), submission.pointCost(), submission.failureCode(), submission.createdAt(),
                submission.recordings().stream().map(Recording::from).toList(),
                submission.status() == SpeakingSubmissionStatus.GRADED ? learnerDisplay(submission.result()) : null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> learnerDisplay(Map<String, Object> stored) {
        if (stored == null) return null;
        Object display = stored.get("display");
        return display instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }
}
