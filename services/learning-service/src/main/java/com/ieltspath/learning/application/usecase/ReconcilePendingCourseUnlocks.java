package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.PendingCourseUnlockWriter;
import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReconcilePendingCourseUnlocks {
    private final LearnerCourseUnlockRepository unlocks;
    private final AccessClient access;
    private final PendingCourseUnlockWriter intentWriter;

    public ReconcilePendingCourseUnlocks(LearnerCourseUnlockRepository unlocks, AccessClient access,
                                         PendingCourseUnlockWriter intentWriter) {
        this.unlocks = unlocks;
        this.access = access;
        this.intentWriter = intentWriter;
    }

    /** Returns true when every pending debit was reconciled or cancelled; unavailable Access leaves rows pending. */
    public boolean reconcile(UUID userId) {
        boolean complete = true;
        for (LearnerCourseUnlock unlock : unlocks.findPending(userId)) {
            try {
                UUID ledgerId = unlock.pointCost() == 0 ? null : debit(unlock);
                unlocks.activate(unlock.requestId(), ledgerId);
            } catch (InsufficientPointsException exception) {
                intentWriter.cancel(unlock.requestId());
            } catch (AccessUnavailableException exception) {
                complete = false;
            }
        }
        return complete;
    }

    public UUID debit(LearnerCourseUnlock unlock) {
        UUID ledgerId = access.debit(unlock.userId(), unlock.pointCost(), "COURSE_UNLOCK", unlock.requestId(),
                unlock.requestId().toString(), "Course unlock "
                        + (unlock.skill() == null ? "COMBO" : unlock.skill().name()));
        if (ledgerId == null) throw new AccessUnavailableException(null);
        return ledgerId;
    }
}
