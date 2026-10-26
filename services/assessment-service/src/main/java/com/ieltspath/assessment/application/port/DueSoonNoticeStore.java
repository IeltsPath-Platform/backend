package com.ieltspath.assessment.application.port;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** The human grading jobs that will be late soon and whose examiner has not been reminded yet. */
public interface DueSoonNoticeStore {
    /**
     * Locks up to {@code limit} open jobs whose examiner was chosen at or before {@code chosenBefore} but after
     * {@code chosenAfter} (not late yet) and who were never reminded. Rows another instance holds are skipped. QA samples
     * are never included.
     */
    List<DueSoonJob> claimUnreminded(Instant chosenBefore, Instant chosenAfter, int limit);

    /**
     * Records that the examiner was reminded. Also covers the other open jobs of the same attempts: one examiner grades a
     * whole attempt, so they are reminded once.
     */
    void markReminded(Collection<UUID> jobIds, Collection<UUID> attemptIds, Instant at);

    record DueSoonJob(UUID id, UUID examinerId, UUID attemptId, String skill, Instant chosenAt) {}
}
