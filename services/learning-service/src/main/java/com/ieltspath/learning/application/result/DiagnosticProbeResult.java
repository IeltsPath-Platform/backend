package com.ieltspath.learning.application.result;

import java.util.List;
import java.util.UUID;

/** The questions of one probe; no answer spec, explanation or transcript. Empty unless the probe is OPEN. */
public record DiagnosticProbeResult(UUID sessionId, UUID probeId, UUID knowledgePointId, int depth, String status,
                                    String passage, ReviewResult.Audio audio, List<LessonResult.Question> questions) {}
