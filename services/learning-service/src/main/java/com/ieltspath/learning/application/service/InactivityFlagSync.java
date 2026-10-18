package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import com.ieltspath.learning.domain.service.AnomalyRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/** Opens INACTIVE flags for learners who have stopped answering while still on a course. One statement per run. */
@Component
public class InactivityFlagSync {
    private final StaffInsightRepository insight;
    private final int inactiveDays;
    private final Clock clock = Clock.systemUTC();

    public InactivityFlagSync(StaffInsightRepository insight,
                              @Value("${learning.flags.inactive-days:3}") int inactiveDays) {
        this.insight = insight;
        this.inactiveDays = inactiveDays;
    }

    /** Returns how many flags were opened in this run. Runs inside the caller's transaction. */
    public int run() {
        Instant now = clock.instant();
        return insight.openInactivityFlags(AnomalyRules.inactiveCutoff(now, inactiveDays), now);
    }
}
