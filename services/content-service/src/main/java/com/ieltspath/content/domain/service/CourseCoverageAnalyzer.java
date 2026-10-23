package com.ieltspath.content.domain.service;

import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.CourseCatalogFacts;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis.Gap;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis.Issue;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import com.ieltspath.content.domain.vo.CourseDraftStructure.LessonPlan;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Checks a course draft against the catalogue: whether it teaches the knowledge points it should, in an order that
 * respects their prerequisites, with enough practice material behind them, and without using names or packages that
 * are already taken. It reads nothing itself; the catalogue facts are handed in.
 */
public class CourseCoverageAnalyzer {

    /** A knowledge point wants at least this many unlinked published practice sets. */
    public static final int WANTED_PRACTICE_SETS = 3;

    public CourseDraftAnalysis analyze(CourseDraftStructure draft, CourseCatalogFacts facts) {
        List<Issue> errors = new ArrayList<>();
        List<Issue> warnings = new ArrayList<>();
        List<Gap> gaps = new ArrayList<>();

        List<LessonPlan> lessons = draft.lessonsInOrder();
        Set<UUID> used = usedKnowledgePoints(lessons);

        checkKnowledgePoints(used, facts, errors);
        checkQuestionRefs(lessons, facts, errors);
        checkPracticeSets(lessons, facts, errors);
        checkMilestones(draft, facts, errors);
        checkNames(draft, facts, errors);
        checkPrerequisiteOrder(draft, lessons, facts, errors);
        CourseDraftAnalysis.Coverage coverage = coverage(used, facts, warnings);
        checkGaps(used, facts, gaps);
        return new CourseDraftAnalysis(errors, warnings, gaps, coverage);
    }

    private static Set<UUID> usedKnowledgePoints(List<LessonPlan> lessons) {
        Set<UUID> used = new LinkedHashSet<>();
        lessons.forEach(lesson -> used.addAll(lesson.allKnowledgePointIds()));
        return used;
    }

    private static void checkKnowledgePoints(Set<UUID> used, CourseCatalogFacts facts, List<Issue> errors) {
        for (UUID id : used) {
            CourseCatalogFacts.KnowledgePointFact point = facts.knowledgePoints().get(id);
            if (point == null) {
                errors.add(new Issue("KNOWLEDGE_POINT_UNKNOWN", id.toString(), null));
            } else if (!point.active()) {
                errors.add(new Issue("KNOWLEDGE_POINT_INACTIVE", id.toString(), null));
            }
        }
    }

    private static void checkQuestionRefs(List<LessonPlan> lessons, CourseCatalogFacts facts, List<Issue> errors) {
        Set<String> reported = new HashSet<>();
        for (LessonPlan lesson : lessons) {
            lesson.blocks().stream().filter(block -> block.type() == BlockType.EXERCISE)
                    .flatMap(block -> block.questionRefs().stream())
                    .filter(ref -> !facts.resolvedQuestionRefs().containsKey(ref) && reported.add(ref))
                    .forEach(ref -> errors.add(new Issue("QUESTION_REF_UNKNOWN", ref, null)));
        }
    }

    private static void checkPracticeSets(List<LessonPlan> lessons, CourseCatalogFacts facts, List<Issue> errors) {
        Set<UUID> chosen = new HashSet<>();
        for (LessonPlan lesson : lessons) {
            for (UUID id : lesson.practiceSetPackageIds()) {
                CourseCatalogFacts.PackageFact pkg = facts.packages().get(id);
                if (pkg == null) {
                    errors.add(new Issue("PACKAGE_UNKNOWN", id.toString(), null));
                } else if (!"PRACTICE_SET".equals(pkg.type())) {
                    errors.add(new Issue("PACKAGE_WRONG_TYPE", id.toString(), null));
                } else if (!pkg.published()) {
                    errors.add(new Issue("PACKAGE_NOT_PUBLISHED", id.toString(), null));
                } else if (pkg.linked() || !chosen.add(id)) {
                    errors.add(new Issue("PACKAGE_ALREADY_LINKED", id.toString(), null));
                }
            }
        }
    }

    private static void checkMilestones(CourseDraftStructure draft, CourseCatalogFacts facts, List<Issue> errors) {
        Set<UUID> seen = new HashSet<>();
        for (CourseDraftStructure.MilestonePlan milestone : draft.practiceTestMilestones()) {
            UUID id = milestone.packageId();
            CourseCatalogFacts.PackageFact pkg = facts.packages().get(id);
            if (pkg == null) {
                errors.add(new Issue("PACKAGE_UNKNOWN", id.toString(), null));
            } else if (!"PRACTICE_TEST".equals(pkg.type())) {
                errors.add(new Issue("PACKAGE_WRONG_TYPE", id.toString(), null));
            } else if (!pkg.published()) {
                errors.add(new Issue("PACKAGE_NOT_PUBLISHED", id.toString(), null));
            } else if (facts.milestonePackagesLinkedElsewhere().contains(id) || !seen.add(id)) {
                errors.add(new Issue("PACKAGE_ALREADY_LINKED", id.toString(), null));
            }
        }
    }

    private static void checkNames(CourseDraftStructure draft, CourseCatalogFacts facts, List<Issue> errors) {
        if (draft.course() != null) {
            if (facts.takenCourseCodes().contains(draft.course().code())) {
                errors.add(new Issue(Issue.CODE_TAKEN, draft.course().code(), "COURSE"));
            }
            if (facts.bandTaken()) {
                errors.add(new Issue(Issue.BAND_TAKEN, draft.course().bandLevel().toPlainString(), null));
            }
        }
        for (CourseDraftStructure.TopicPlan topic : draft.topics()) {
            if (facts.takenTopicCodes().contains(topic.code())) {
                errors.add(new Issue(Issue.CODE_TAKEN, topic.code(), "TOPIC"));
            }
            if (topic.sortOrder() != null && facts.takenTopicSortOrders().contains(topic.sortOrder())) {
                errors.add(new Issue(Issue.TOPIC_SORT_ORDER_TAKEN, topic.code(), topic.sortOrder().toString()));
            }
            for (LessonPlan lesson : topic.lessons()) {
                if (facts.takenLessonCodes().contains(lesson.code())) {
                    errors.add(new Issue(Issue.CODE_TAKEN, lesson.code(), "LESSON"));
                }
            }
            if (topic.topicTest() != null && facts.takenPackageCodes().contains(topic.topicTest().code())) {
                errors.add(new Issue(Issue.CODE_TAKEN, topic.topicTest().code(), "PACKAGE"));
            }
        }
        if (draft.courseTest() != null && facts.takenPackageCodes().contains(draft.courseTest().code())) {
            errors.add(new Issue(Issue.CODE_TAKEN, draft.courseTest().code(), "PACKAGE"));
        }
    }

    /**
     * A knowledge point's prerequisite must be taught in an earlier lesson of the course, or by an existing course of a
     * lower band. The first lesson that mentions a point is where it is taught; a prerequisite in the same lesson is not
     * earlier.
     */
    private static void checkPrerequisiteOrder(CourseDraftStructure draft, List<LessonPlan> lessons,
                                               CourseCatalogFacts facts, List<Issue> errors) {
        Map<UUID, Integer> firstLesson = new java.util.LinkedHashMap<>();
        for (int index = 0; index < lessons.size(); index++) {
            for (UUID id : lessons.get(index).allKnowledgePointIds()) {
                firstLesson.putIfAbsent(id, index);
            }
        }
        BigDecimal courseBand = draft.course() == null ? null : draft.course().bandLevel();
        for (Map.Entry<UUID, Integer> point : firstLesson.entrySet()) {
            for (UUID prerequisite : facts.prerequisites().getOrDefault(point.getKey(), Set.of())) {
                Integer earlier = firstLesson.get(prerequisite);
                boolean taughtBefore = earlier != null && earlier < point.getValue();
                BigDecimal lowerBand = facts.lowestBandTaught().get(prerequisite);
                boolean taughtLower = lowerBand != null && courseBand != null && lowerBand.compareTo(courseBand) < 0;
                if (!taughtBefore && !taughtLower) {
                    errors.add(new Issue("PREREQUISITE_ORDER", point.getKey().toString(), prerequisite.toString()));
                }
            }
        }
    }

    private static CourseDraftAnalysis.Coverage coverage(Set<UUID> used, CourseCatalogFacts facts,
                                                         List<Issue> warnings) {
        int covered = 0;
        for (UUID expected : facts.expectedKnowledgePoints().stream().sorted().toList()) {
            if (used.contains(expected)) {
                covered++;
            } else {
                warnings.add(new Issue("KP_NOT_COVERED", expected.toString(), null));
            }
        }
        return new CourseDraftAnalysis.Coverage(facts.expectedKnowledgePoints().size(), covered);
    }

    private static void checkGaps(Set<UUID> used, CourseCatalogFacts facts, List<Gap> gaps) {
        for (UUID id : used) {
            CourseCatalogFacts.KnowledgePointFact point = facts.knowledgePoints().get(id);
            if (point == null || !point.active()) {
                continue;
            }
            int sets = facts.unlinkedPracticeSets().getOrDefault(id, 0);
            if (sets == 0) {
                gaps.add(new Gap(id, "NO_PRACTICE_SET"));
            } else if (sets < WANTED_PRACTICE_SETS) {
                gaps.add(new Gap(id, "FEW_PRACTICE_SETS"));
            }
            if (facts.withoutExerciseQuestion().contains(id)) {
                gaps.add(new Gap(id, "NO_EXERCISE_QUESTION"));
            }
        }
    }
}
