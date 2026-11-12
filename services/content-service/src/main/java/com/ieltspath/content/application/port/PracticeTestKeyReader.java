package com.ieltspath.content.application.port;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Maps what a learner's records point at to the practice test the catalog lists: a package version (a Speaking
 * submission keeps one) or a Writing question version (a free essay keeps one). The catalog knows a test by its package
 * and part, which stay the same when a newer version of the package is published, so a record made on an older version
 * still belongs to the same test.
 */
public interface PracticeTestKeyReader {
    /** Ids that match no practice test are left out of the answer. */
    Keys find(List<UUID> packageVersionIds, List<UUID> questionVersionIds);

    /** {@code packageByVersion} is the package of each package version; {@code byQuestion} the test of each Writing question. */
    record Keys(Map<UUID, UUID> packageByVersion, Map<UUID, QuestionKey> byQuestion) {}

    record QuestionKey(UUID packageId, Integer partNo) {}
}
