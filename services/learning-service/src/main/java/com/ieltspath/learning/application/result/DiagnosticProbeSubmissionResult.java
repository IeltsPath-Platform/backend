package com.ieltspath.learning.application.result;

import java.util.List;
import java.util.UUID;

/**
 * An answered probe shows its solutions and the audio transcript, passed or not. {@code sessionStatus} turns
 * CONCLUDED once the last probe is answered; {@code conclusionKnowledgePointIds} are then the gap points.
 */
public record DiagnosticProbeSubmissionResult(UUID probeId, boolean passed, int correct, int total,
                                              List<SubmissionResult.AnswerResult> results, String transcript,
                                              String sessionStatus, List<UUID> conclusionKnowledgePointIds) {}
