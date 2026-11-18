package com.ieltspath.content.application.port;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The published practice sets that belong to no lesson, which an author may attach to a lesson of a course draft. */
public interface UnlinkedPracticeSetReader {

    record PracticeSetRef(UUID packageId, String code) {
    }

    /** Up to {@code perPoint} published practice sets that belong to no lesson, for each point they measure. */
    Map<UUID, List<PracticeSetRef>> practiceSets(Collection<UUID> pointIds, int perPoint);
}
