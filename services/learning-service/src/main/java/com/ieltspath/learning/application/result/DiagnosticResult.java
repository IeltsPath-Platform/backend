package com.ieltspath.learning.application.result;

import java.util.List;
import java.util.UUID;

/**
 * A diagnosis with its result matrix: one row per probe, in the order the probes were opened. {@code conclusion} is
 * empty while PROBING; when no probe failed it holds the root point itself ({@code rootIsGap = false} then).
 * {@code waitingOnReviewIds} are the reviews of the gap points the root review still waits for.
 */
public record DiagnosticResult(UUID sessionId, UUID rootReviewId, UUID rootKnowledgePointId, String status,
                               List<ProbeRow> probes, List<Point> conclusion, boolean rootIsGap,
                               List<UUID> waitingOnReviewIds) {
    public record Point(UUID knowledgePointId, String code, String name) {}

    /** {@code result} is PASSED or FAILED once answered, UNKNOWN for a probe without a set, else null. */
    public record ProbeRow(UUID probeId, Point knowledgePoint, UUID parentKnowledgePointId, int depth, String status,
                           Integer correct, Integer total, String result) {}
}
