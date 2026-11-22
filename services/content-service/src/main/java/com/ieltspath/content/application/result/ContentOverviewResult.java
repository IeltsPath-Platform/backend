package com.ieltspath.content.application.result;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Counts of the content this service owns, for the overview of the admin area. */
public record ContentOverviewResult(
        KnowledgePointCounts knowledgePoints,
        long activeKnowledgePointsWithoutPublishedQuestion,
        long draftQuestionVersions,
        Map<String, Long> lessons,
        List<CoverageCell> coverage,
        Map<String, Long> questionVersions,
        Map<String, Long> packages,
        long staleDraftQuestionVersions,
        int staleDraftAfterDays) {

    /** {@code bySkill} and {@code byCefr} count the active points only; a point without a level is under {@code NONE}. */
    public record KnowledgePointCounts(long total, Map<String, Long> byStatus, Map<String, Long> bySkill,
                                       Map<String, Long> byCefr) {}

    /**
     * Active knowledge points of one skill and one CEFR level, and how many of them have no published question or exactly
     * one. A skill or level a point does not have is {@code NONE}.
     */
    public record CoverageCell(String skill, String cefrLevel, long activeKnowledgePoints,
                               long withoutPublishedQuestion, long withOnePublishedQuestion) {}

    /** What a question version is about, in a few words an author recognises: the start of its stem. */
    public record QuestionLabel(UUID questionVersionId, UUID questionId, String label, String skill, String questionType) {}
}