package com.ieltspath.content.application.result;

import java.util.Map;

/** Counts of the content this service owns, for the overview of the admin area. */
public record ContentOverviewResult(
        KnowledgePointCounts knowledgePoints,
        long activeKnowledgePointsWithoutPublishedQuestion,
        long draftQuestionVersions,
        Map<String, Long> lessons) {

    /** {@code bySkill} and {@code byCefr} count the active points only; a point without a level is under {@code NONE}. */
    public record KnowledgePointCounts(long total, Map<String, Long> byStatus, Map<String, Long> bySkill,
                                       Map<String, Long> byCefr) {}
}
