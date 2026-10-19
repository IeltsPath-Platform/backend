package com.ieltspath.learning.application.service;

import com.ieltspath.events.points.PointsGrantSettled;

import com.ieltspath.learning.application.port.PointsGrantPublisher;
import com.ieltspath.learning.domain.repository.RewardGrantRepository;
import com.ieltspath.learning.domain.repository.RewardGrantRepository.PendingGrant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sends the points queued by rewards to Access and records Access's answer. A grant is sent when it is new, and again if
 * no answer came within the resend delay; Access credits a grant id once, so a resend never pays twice. A grant sent too
 * many times without an answer is given up and kept as failed. Grant contents are never logged.
 */
@Component
public class RewardGrantService {
    private static final Logger log = LoggerFactory.getLogger(RewardGrantService.class);
    static final Set<String> ANSWERS = PointsGrantSettled.STATUSES;
    static final String NO_ANSWER = "NO_SETTLEMENT";

    private final RewardGrantRepository grants;
    private final PointsGrantPublisher publisher;
    private final int batch;
    private final Duration resendAfter;
    private final int maxSends;
    private final Clock clock = Clock.systemUTC();

    public RewardGrantService(RewardGrantRepository grants, PointsGrantPublisher publisher,
                              @Value("${learning.rewards.batch:100}") int batch,
                              @Value("${learning.rewards.resend-after:PT5M}") Duration resendAfter,
                              @Value("${learning.rewards.max-sends:10}") int maxSends) {
        this.grants = grants;
        this.publisher = publisher;
        this.batch = batch;
        this.resendAfter = resendAfter;
        this.maxSends = maxSends;
    }

    /** Sends the grants that are due; returns how many were sent. Runs inside the caller's transaction. */
    public int sendDue() {
        Instant now = clock.instant();
        List<PendingGrant> due = grants.claimDue(batch, now.minus(resendAfter));
        int sent = 0;
        for (PendingGrant grant : due) {
            if (grant.attempts() >= maxSends) {
                grants.markFailed(grant.id(), NO_ANSWER, now);
                log.warn("Reward grant given up without an answer: grantId={}, reason={}, sends={}", grant.id(), NO_ANSWER,
                        grant.attempts());
                continue;
            }
            try {
                publisher.publish(grant);
                grants.markSent(grant.id(), now);
                sent++;
            } catch (RuntimeException failure) {
                // The broker did not take the grant (unreachable, refused, or no queue bound): it was not sent, so the
                // grant stays due and its send count does not grow.
                log.warn("Reward grant not sent: grantId={}, errorType={}", grant.id(),
                        failure.getClass().getSimpleName());
            }
        }
        return sent;
    }

    /** Records Access's answer. An unknown status or a grant already settled changes nothing. */
    public boolean settle(UUID grantId, String status, String error) {
        if (status == null || !ANSWERS.contains(status)) {
            log.warn("Reward grant answer with an unknown status: grantId={}", grantId);
            return false;
        }
        return grants.settle(grantId, status, error, clock.instant());
    }
}
