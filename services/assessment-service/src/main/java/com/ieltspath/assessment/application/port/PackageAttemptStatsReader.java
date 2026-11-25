package com.ieltspath.assessment.application.port;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** How often each package was attempted, for the author screens that manage tests. */
public interface PackageAttemptStatsReader {

    /**
     * @param started  attempts begun on any version of the package
     * @param submitted attempts that were submitted
     * @param averageBand average overall band of the latest completed result of each attempt; null when none is graded
     */
    record PackageStats(UUID packageId, long started, long submitted, BigDecimal averageBand) {}

    /** One row per package that has at least one attempt; a package without attempts is not returned. */
    List<PackageStats> forPackages(Collection<UUID> packageIds);
}
