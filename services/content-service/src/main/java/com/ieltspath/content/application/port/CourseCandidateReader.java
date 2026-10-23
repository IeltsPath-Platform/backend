package com.ieltspath.content.application.port;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** What the course generator may choose from: active knowledge points and the practice material behind them. */
public interface CourseCandidateReader {

    record Point(UUID id, String code, String name, String description, String cefrLevel, String skill) {
    }

    record PracticeSetRef(UUID packageId, String code) {
    }

    /** Active points at this CEFR level whose skill is one of {@code skills}, or has none or {@code ALL}. */
    List<Point> pool(String cefrLevel, Set<String> skills);

    /** The active ones among these points. Unknown and inactive ids are left out. */
    List<Point> activeByIds(Collection<UUID> ids);

    /** Active direct prerequisites of each point, restricted to active points. Points without any are absent. */
    Map<UUID, Set<UUID>> prerequisites(Collection<UUID> ids);

    /** Up to {@code perPoint} published practice sets that belong to no lesson, for each point they measure. */
    Map<UUID, List<PracticeSetRef>> practiceSets(Collection<UUID> pointIds, int perPoint);

    /** Up to {@code perPoint} published learning question versions for each point they measure. */
    Map<UUID, List<UUID>> exerciseQuestions(Collection<UUID> pointIds, int perPoint);

    /** The highest sort order among top-level topics, or 0 when there are none. New topics go after it. */
    int highestRootTopicSortOrder();
}
