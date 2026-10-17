package com.ieltspath.learning.application.port;

import com.ieltspath.learning.domain.repository.RewardGrantRepository.PendingGrant;

/** Sends a points grant to Access. Access credits it once per grant id, so sending again is safe. */
public interface PointsGrantPublisher {
    /** Throws a runtime exception when the message could not be handed to the broker. */
    void publish(PendingGrant grant);
}
