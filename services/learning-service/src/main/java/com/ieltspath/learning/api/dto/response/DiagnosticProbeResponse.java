package com.ieltspath.learning.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ieltspath.learning.application.result.DiagnosticProbeResult;
import com.ieltspath.learning.application.result.DiagnosticProbeSubmissionResult;

import java.util.List;
import java.util.UUID;

/**
 * The questions of one probe use the lesson allowlist plus {@code hint}: no answer spec, explanation or transcript.
 * {@code questions} is empty unless the probe is OPEN.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiagnosticProbeResponse(UUID sessionId, UUID probeId, UUID knowledgePointId, int depth, String status,
                                      String passage, ReviewResponse.Audio audio,
                                      List<LessonResponse.Question> questions) {
    public static DiagnosticProbeResponse from(DiagnosticProbeResult result) {
        return new DiagnosticProbeResponse(result.sessionId(), result.probeId(), result.knowledgePointId(),
                result.depth(), result.status(), result.passage(),
                result.audio() == null ? null
                        : new ReviewResponse.Audio(result.audio().mediaUrl(), result.audio().durationSeconds()),
                ReviewResponse.questions(result.questions()));
    }

    /** An answered probe shows its solutions and the audio transcript, passed or not. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Submission(UUID probeId, boolean passed, int correct, int total,
                             List<SubmissionResponse.SolvedAnswer> results, String transcript, String sessionStatus,
                             List<UUID> conclusionKnowledgePointIds) {
        public static Submission from(DiagnosticProbeSubmissionResult result) {
            return new Submission(result.probeId(), result.passed(), result.correct(), result.total(),
                    result.results().stream().map(answer -> new SubmissionResponse.SolvedAnswer(
                            answer.questionVersionId(), answer.correct(), answer.correctAnswer(),
                            answer.explanation())).toList(),
                    result.transcript(), result.sessionStatus(), result.conclusionKnowledgePointIds());
        }
    }
}
