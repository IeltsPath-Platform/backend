package com.ieltspath.assessment.application.port;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** The human grading jobs whose chosen examiner is late and whose learner has not been told yet. */
public interface OverdueNoticeStore {
    /**
     * Locks up to {@code limit} open jobs the learner chose an examiner for, chosen at or before {@code chosenBefore},
     * that were never announced as late. Rows another instance holds are skipped. QA samples are never included.
     */
    List<OverdueJob> claimUnannounced(Instant chosenBefore, int limit);

    /**
     * Records that the learner was told. Also covers the other open jobs of the same attempts: one examiner grades a
     * whole attempt, so the learner hears about it once.
     */
    void markAnnounced(Collection<UUID> jobIds, Collection<UUID> attemptIds, Instant at);

    record OverdueJob(UUID id, UUID learnerId, UUID examinerId, UUID attemptId, String skill, Instant chosenAt) {}
}
