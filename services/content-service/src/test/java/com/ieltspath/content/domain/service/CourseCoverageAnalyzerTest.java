package com.ieltspath.content.domain.service;

import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.CourseCatalogFacts;
import com.ieltspath.content.domain.vo.CourseCatalogFacts.KnowledgePointFact;
import com.ieltspath.content.domain.vo.CourseCatalogFacts.PackageFact;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis.Issue;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import com.ieltspath.content.domain.vo.CourseDraftStructure.BlockPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.CourseInfo;
import com.ieltspath.content.domain.vo.CourseDraftStructure.FinalTestPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.LessonPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.MilestonePlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.TopicPlan;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseCoverageAnalyzerTest {

    private final CourseCoverageAnalyzer analyzer = new CourseCoverageAnalyzer();

    private final UUID kpA = UUID.randomUUID();
    private final UUID kpB = UUID.randomUUID();
    private final UUID kpC = UUID.randomUUID();

    /** Mutable stand-in for the catalogue; every known point is active and well stocked unless a test says otherwise. */
    private final Map<UUID, KnowledgePointFact> points = new HashMap<>();
    private final Set<UUID> expected = new HashSet<>();
    private final Map<UUID, Set<UUID>> prerequisites = new HashMap<>();
    private final Map<UUID, BigDecimal> lowerBand = new HashMap<>();
    private final Map<UUID, PackageFact> packages = new HashMap<>();
    private final Set<UUID> milestoneElsewhere = new HashSet<>();
    private final Map<UUID, Integer> sets = new HashMap<>();
    private final Set<UUID> noQuestions = new HashSet<>();
    private final Map<String, UUID> refs = new HashMap<>();
    private final Set<String> takenCourse = new HashSet<>();
    private final Set<String> takenTopic = new HashSet<>();
    private final Set<String> takenLesson = new HashSet<>();
    private final Set<String> takenPackage = new HashSet<>();
    private final Set<Integer> takenSort = new HashSet<>();
    private boolean bandTaken;
    private final Set<UUID> ownedQuestions = new HashSet<>();

    {
        for (UUID id : List.of(kpA, kpB, kpC)) {
            points.put(id, new KnowledgePointFact(id, "KP-" + id.toString().substring(0, 4), "ACTIVE"));
            sets.put(id, 5);
        }
    }

    private CourseCatalogFacts facts() {
        return new CourseCatalogFacts(points, expected, prerequisites, lowerBand, packages, milestoneElsewhere, sets,
                noQuestions, refs, takenCourse, takenTopic, takenLesson, takenPackage, bandTaken, takenSort, ownedQuestions);
    }

    private static LessonPlan lesson(String code, UUID... kps) {
        return new LessonPlan(code, "Lesson " + code, null, List.of(kps), List.of(), List.of());
    }

    private static CourseDraftStructure draft(LessonPlan... lessons) {
        return new CourseDraftStructure(new CourseInfo("C1", "Course", new BigDecimal("6.5")),
                List.of(new TopicPlan("T1", "Topic", Skill.READING, 970, List.of(lessons),
                        new FinalTestPlan("T1-TEST", "Topic test"))),
                new FinalTestPlan("C1-TEST", "Course test"), List.of(), List.of());
    }

    private static List<String> codes(List<Issue> issues) {
        return issues.stream().map(Issue::code).toList();
    }

    @Test
    void aWellFormedDraftHasNoErrorsAndFullCoverage() {
        expected.addAll(List.of(kpA, kpB));
        prerequisites.put(kpB, Set.of(kpA));

        CourseDraftAnalysis result = analyzer.analyze(draft(lesson("L1", kpA), lesson("L2", kpB)), facts());

        assertTrue(result.valid());
        assertEquals(List.of(), result.warnings());
        assertEquals(List.of(), result.gaps());
        assertEquals(new CourseDraftAnalysis.Coverage(2, 2), result.coverage());
    }

    @Test
    void anExpectedPointNoLessonTeachesIsAWarningNotAnError() {
        expected.addAll(List.of(kpA, kpB, kpC));

        CourseDraftAnalysis result = analyzer.analyze(draft(lesson("L1", kpA)), facts());

        assertTrue(result.valid());
        assertEquals(new CourseDraftAnalysis.Coverage(3, 1), result.coverage());
        assertEquals(2, result.warnings().size());
        assertTrue(codes(result.warnings()).stream().allMatch("KP_NOT_COVERED"::equals));
        assertEquals(Set.of(kpB.toString(), kpC.toString()),
                new HashSet<>(result.warnings().stream().map(Issue::subject).toList()));
    }

    @Test
    void aPointTaughtBeforeItsPrerequisiteIsAnError() {
        prerequisites.put(kpB, Set.of(kpA));

        CourseDraftAnalysis result = analyzer.analyze(draft(lesson("L1", kpB), lesson("L2", kpA)), facts());

        assertFalse(result.valid());
        assertEquals(List.of(new Issue("PREREQUISITE_ORDER", kpB.toString(), kpA.toString())), result.errors());
    }

    @Test
    void aPrerequisiteInTheSameLessonIsNotEarlier() {
        prerequisites.put(kpB, Set.of(kpA));

        CourseDraftAnalysis result = analyzer.analyze(draft(lesson("L1", kpA, kpB)), facts());

        assertEquals(List.of("PREREQUISITE_ORDER"), codes(result.errors()));
    }

    @Test
    void aPrerequisiteTaughtByALowerBandCourseIsFine() {
        prerequisites.put(kpB, Set.of(kpA));
        lowerBand.put(kpA, new BigDecimal("5.5"));

        CourseDraftAnalysis result = analyzer.analyze(draft(lesson("L1", kpB)), facts());

        assertTrue(result.valid());
    }

    @Test
    void aPrerequisiteTaughtOnlyByAHigherOrSameBandCourseIsNotEnough() {
        prerequisites.put(kpB, Set.of(kpA));
        lowerBand.put(kpA, new BigDecimal("6.5"));

        assertEquals(List.of("PREREQUISITE_ORDER"),
                codes(analyzer.analyze(draft(lesson("L1", kpB)), facts()).errors()));
    }

    @Test
    void aPrerequisiteTaughtNowhereIsAnError() {
        prerequisites.put(kpB, Set.of(kpA));

        assertEquals(List.of("PREREQUISITE_ORDER"),
                codes(analyzer.analyze(draft(lesson("L1", kpB)), facts()).errors()));
    }

    @Test
    void topicsAreReadInSortOrderNotListOrder() {
        prerequisites.put(kpB, Set.of(kpA));
        CourseDraftStructure listedBackwards = new CourseDraftStructure(
                new CourseInfo("C1", "Course", new BigDecimal("6.5")),
                List.of(new TopicPlan("TB", "Second", Skill.READING, 972, List.of(lesson("LB", kpB)), null),
                        new TopicPlan("TA", "First", Skill.READING, 971, List.of(lesson("LA", kpA)), null)),
                null, List.of(), List.of());

        assertTrue(analyzer.analyze(listedBackwards, facts()).valid());
    }

    @Test
    void unknownAndInactivePointsAreErrors() {
        UUID unknown = UUID.randomUUID();
        points.put(kpC, new KnowledgePointFact(kpC, "KP-C", "INACTIVE"));

        CourseDraftAnalysis result = analyzer.analyze(draft(lesson("L1", unknown, kpC)), facts());

        assertEquals(List.of(new Issue("KNOWLEDGE_POINT_UNKNOWN", unknown.toString(), null),
                new Issue("KNOWLEDGE_POINT_INACTIVE", kpC.toString(), null)), result.errors());
    }

    @Test
    void aQuestionThatBelongsToALessonOrAPackageAlreadyIsAnError() {
        UUID owned = UUID.randomUUID();
        refs.put("owned-ref", owned);
        refs.put("free-ref", UUID.randomUUID());
        ownedQuestions.add(owned);
        LessonPlan withExercises = new LessonPlan("L1", "L", null, List.of(kpA), List.of(
                new BlockPlan(BlockType.EXERCISE, null, List.of(), List.of("owned-ref", "free-ref")),
                new BlockPlan(BlockType.EXERCISE, null, List.of(), List.of("owned-ref"))), List.of());

        CourseDraftAnalysis result = analyzer.analyze(draft(withExercises), facts());

        assertEquals(List.of(new Issue("QUESTION_ALREADY_OWNED", "owned-ref", null)), result.errors());
    }

    @Test
    void aQuestionUsedInTwoBlocksOfTheDraftIsAnErrorOncePerQuestion() {
        UUID shared = UUID.randomUUID();
        refs.put("first", shared);
        refs.put("same-question", shared);
        refs.put("other", UUID.randomUUID());
        LessonPlan one = new LessonPlan("L1", "L", null, List.of(kpA), List.of(
                new BlockPlan(BlockType.EXERCISE, null, List.of(), List.of("first", "other"))), List.of());
        LessonPlan two = new LessonPlan("L2", "L", null, List.of(kpB), List.of(
                new BlockPlan(BlockType.EXERCISE, null, List.of(), List.of("same-question", "same-question"))), List.of());

        CourseDraftAnalysis result = analyzer.analyze(draft(one, two), facts());

        assertEquals(List.of(new Issue("QUESTION_REUSED", "same-question", null)), result.errors());
    }

    @Test
    void unresolvedQuestionReferencesAreReportedOnce() {
        refs.put("known-ref", UUID.randomUUID());
        LessonPlan withExercises = new LessonPlan("L1", "L", null, List.of(kpA), List.of(
                new BlockPlan(BlockType.EXERCISE, null, List.of(), List.of("known-ref", "missing-ref")),
                new BlockPlan(BlockType.EXERCISE, null, List.of(), List.of("missing-ref"))), List.of());

        CourseDraftAnalysis result = analyzer.analyze(draft(withExercises), facts());

        assertEquals(List.of(new Issue("QUESTION_REF_UNKNOWN", "missing-ref", null)), result.errors());
    }

    @Test
    void practiceSetsMustBePublishedPracticeSetsNobodyOwns() {
        UUID free = pkg("PRACTICE_SET", "PUBLISHED", false);
        UUID draftSet = pkg("PRACTICE_SET", "DRAFT", false);
        UUID quiz = pkg("QUIZ", "PUBLISHED", false);
        UUID taken = pkg("PRACTICE_SET", "PUBLISHED", true);
        UUID missing = UUID.randomUUID();
        LessonPlan lesson = new LessonPlan("L1", "L", null, List.of(kpA), List.of(),
                List.of(free, draftSet, quiz, taken, missing));

        CourseDraftAnalysis result = analyzer.analyze(draft(lesson), facts());

        assertEquals(List.of(new Issue("PACKAGE_NOT_PUBLISHED", draftSet.toString(), null),
                new Issue("PACKAGE_WRONG_TYPE", quiz.toString(), null),
                new Issue("PACKAGE_ALREADY_LINKED", taken.toString(), null),
                new Issue("PACKAGE_UNKNOWN", missing.toString(), null)), result.errors());
    }

    @Test
    void aPracticeSetChosenTwiceIsLinkedTwice() {
        UUID free = pkg("PRACTICE_SET", "PUBLISHED", false);

        CourseDraftAnalysis result = analyzer.analyze(draft(
                new LessonPlan("L1", "L", null, List.of(kpA), List.of(), List.of(free)),
                new LessonPlan("L2", "L", null, List.of(kpB), List.of(), List.of(free))), facts());

        assertEquals(List.of(new Issue("PACKAGE_ALREADY_LINKED", free.toString(), null)), result.errors());
    }

    @Test
    void milestonesNeedPublishedPracticeTestsNotUsedByAnotherCourse() {
        UUID good = pkg("PRACTICE_TEST", "PUBLISHED", false);
        UUID wrongType = pkg("PRACTICE_SET", "PUBLISHED", false);
        UUID elsewhere = pkg("PRACTICE_TEST", "PUBLISHED", false);
        milestoneElsewhere.add(elsewhere);
        CourseDraftStructure base = draft(lesson("L1", kpA));
        CourseDraftStructure withMilestones = new CourseDraftStructure(base.course(), base.topics(), base.courseTest(),
                List.of(new MilestonePlan(good, 1), new MilestonePlan(wrongType, 1), new MilestonePlan(elsewhere, 1)),
                List.of());

        CourseDraftAnalysis result = analyzer.analyze(withMilestones, facts());

        assertEquals(List.of(new Issue("PACKAGE_WRONG_TYPE", wrongType.toString(), null),
                new Issue("PACKAGE_ALREADY_LINKED", elsewhere.toString(), null)), result.errors());
    }

    @Test
    void namesAndPositionsAlreadyTakenAreConflicts() {
        takenCourse.add("C1");
        takenTopic.add("T1");
        takenLesson.add("L1");
        takenPackage.addAll(List.of("T1-TEST", "C1-TEST"));
        takenSort.add(970);
        bandTaken = true;

        CourseDraftAnalysis result = analyzer.analyze(draft(lesson("L1", kpA)), facts());

        assertEquals(7, result.conflicts().size());
        assertEquals(result.errors(), result.conflicts());
        assertTrue(result.errors().contains(new Issue("CODE_TAKEN", "C1", "COURSE")));
        assertTrue(result.errors().contains(new Issue("BAND_TAKEN", "6.5", null)));
        assertTrue(result.errors().contains(new Issue("TOPIC_SORT_ORDER_TAKEN", "T1", "970")));
        assertTrue(result.errors().contains(new Issue("CODE_TAKEN", "L1", "LESSON")));
    }

    @Test
    void invalidIsNotAConflict() {
        prerequisites.put(kpB, Set.of(kpA));

        CourseDraftAnalysis result = analyzer.analyze(draft(lesson("L1", kpB)), facts());

        assertFalse(result.valid());
        assertEquals(List.of(), result.conflicts());
    }

    @Test
    void thinStockIsReportedAsGapsAndNeverAsErrors() {
        sets.put(kpA, 0);
        sets.put(kpB, 2);
        noQuestions.add(kpC);

        CourseDraftAnalysis result = analyzer.analyze(
                draft(lesson("L1", kpA), lesson("L2", kpB), lesson("L3", kpC)), facts());

        assertTrue(result.valid());
        assertEquals(List.of(new CourseDraftAnalysis.Gap(kpA, "NO_PRACTICE_SET"),
                new CourseDraftAnalysis.Gap(kpB, "FEW_PRACTICE_SETS"),
                new CourseDraftAnalysis.Gap(kpC, "NO_EXERCISE_QUESTION")), result.gaps());
    }

    @Test
    void aPointCanHaveSeveralGaps() {
        sets.put(kpA, 1);
        noQuestions.add(kpA);

        CourseDraftAnalysis result = analyzer.analyze(draft(lesson("L1", kpA)), facts());

        assertEquals(List.of(new CourseDraftAnalysis.Gap(kpA, "FEW_PRACTICE_SETS"),
                new CourseDraftAnalysis.Gap(kpA, "NO_EXERCISE_QUESTION")), result.gaps());
    }

    @Test
    void anEmptyDraftIsValidAndCoversNothing() {
        expected.add(kpA);

        CourseDraftAnalysis result = analyzer.analyze(CourseDraftStructure.empty(), facts());

        assertTrue(result.valid());
        assertEquals(new CourseDraftAnalysis.Coverage(1, 0), result.coverage());
    }

    @Test
    void pointsUsedInBlocksCountAsTaught() {
        expected.add(kpB);
        LessonPlan lesson = new LessonPlan("L1", "L", null, List.of(kpA), List.of(
                new BlockPlan(BlockType.TEXT, "text", List.of(kpB), List.of())), List.of());

        CourseDraftAnalysis result = analyzer.analyze(draft(lesson), facts());

        assertEquals(new CourseDraftAnalysis.Coverage(1, 1), result.coverage());
    }

    private UUID pkg(String type, String status, boolean linked) {
        UUID id = UUID.randomUUID();
        packages.put(id, new PackageFact(id, "P-" + id.toString().substring(0, 4), type, status,
                linked ? UUID.randomUUID() : null, null, null));
        return id;
    }
}
