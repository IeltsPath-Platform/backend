package com.ieltspath.content.application.service;

import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.application.port.CourseCandidateReader;
import com.ieltspath.content.application.port.CourseCandidateReader.Point;
import com.ieltspath.content.application.port.CourseCandidateReader.PracticeSetRef;
import com.ieltspath.content.application.result.CourseCandidates;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseCandidateSelectorTest {

    /** A catalogue held in memory. Everything is active; levels, prerequisites and stock are set per test. */
    private static final class FakeReader implements CourseCandidateReader {
        final Map<UUID, Point> points = new java.util.LinkedHashMap<>();
        final Map<UUID, Set<UUID>> prerequisites = new HashMap<>();
        final Map<UUID, List<PracticeSetRef>> sets = new HashMap<>();
        final Map<UUID, List<UUID>> questions = new HashMap<>();

        Point add(String code, String name, String description, String level) {
            Point point = new Point(UUID.randomUUID(), code, name, description, level, "READING");
            points.put(point.id(), point);
            return point;
        }

        @Override public List<Point> pool(String cefrLevel, Set<String> skills) {
            return points.values().stream().filter(point -> cefrLevel != null && cefrLevel.equals(point.cefrLevel()))
                    .toList();
        }

        @Override public List<Point> activeByIds(Collection<UUID> ids) {
            return ids.stream().filter(points::containsKey).map(points::get).toList();
        }

        @Override public Map<UUID, Set<UUID>> prerequisites(Collection<UUID> ids) {
            Map<UUID, Set<UUID>> result = new HashMap<>();
            ids.forEach(id -> { if (prerequisites.containsKey(id)) result.put(id, prerequisites.get(id)); });
            return result;
        }

        @Override public Map<UUID, List<PracticeSetRef>> practiceSets(Collection<UUID> ids, int perPoint) {
            Map<UUID, List<PracticeSetRef>> result = new HashMap<>();
            ids.forEach(id -> { if (sets.containsKey(id)) result.put(id, sets.get(id)); });
            return result;
        }

        @Override public Map<UUID, List<UUID>> exerciseQuestions(Collection<UUID> ids, int perPoint) {
            Map<UUID, List<UUID>> result = new HashMap<>();
            ids.forEach(id -> { if (questions.containsKey(id)) result.put(id, questions.get(id)); });
            return result;
        }

        @Override public int highestRootTopicSortOrder() { return 0; }
    }

    private final FakeReader reader = new FakeReader();
    private final ContentAiProperties properties = new ContentAiProperties();
    private final CourseCandidateSelector selector = new CourseCandidateSelector(reader, properties);

    private static CourseDraftRequest request(List<String> topics, List<UUID> ids) {
        return new CourseDraftRequest(new BigDecimal("6.5"), List.of(Skill.READING), topics, ids, 4, null);
    }

    private static List<String> codes(CourseCandidates result) {
        return result.points().stream().map(CourseCandidates.Candidate::code).toList();
    }

    @Test
    void topicPhrasesMatchByNameCodeAndDescriptionIgnoringCaseAndAccents() {
        reader.add("R_PARA", "Paraphrase", "Rewriting sentences", "C1");
        reader.add("R_TFNG", "Not Given", "Distinguish false from not given", "C1");
        reader.add("R_OTHER", "Skimming", "Reading fast", "C1");

        CourseCandidates result = selector.select(request(List.of("PARAPHRASE", "true false not given"), List.of()));

        assertEquals(List.of("R_PARA"), codes(result));
        // "true false not given" needs every word, and "true" is nowhere: it matches nothing.
        assertEquals(List.of("true false not given"), result.unmatchedTopics());
    }

    @Test
    void aPhraseThatMatchesNothingIsReportedAndNoPointIsMadeUp() {
        reader.add("R_PARA", "Paraphrase", null, "C1");

        CourseCandidates result = selector.select(request(List.of("Graph description"), List.of()));

        assertTrue(result.points().isEmpty());
        assertEquals(List.of("Graph description"), result.unmatchedTopics());
    }

    @Test
    void pointsOfAnotherLevelAreNotMatchedByPhrase() {
        reader.add("R_BASIC", "Paraphrase basics", null, "B2");

        assertTrue(selector.select(request(List.of("paraphrase"), List.of())).points().isEmpty());
    }

    @Test
    void explicitIdsAreTakenAsTheyAre() {
        Point asked = reader.add("R_A", "Anything", null, "C1");

        CourseCandidates result = selector.select(request(List.of(), List.of(asked.id(), UUID.randomUUID())));

        assertEquals(List.of("R_A"), codes(result));
        assertFalse(result.points().get(0).foundation());
    }

    @Test
    void lowerLevelPrerequisitesJoinAsFoundationAndComeFirst() {
        Point foundation = reader.add("R_BASE", "Sentence structure", null, "B2");
        Point asked = reader.add("R_PARA", "Paraphrase", null, "C1");
        Point sameLevel = reader.add("R_SAME", "Synonyms", null, "C1");
        reader.prerequisites.put(asked.id(), Set.of(foundation.id(), sameLevel.id()));

        CourseCandidates result = selector.select(request(List.of(), List.of(asked.id())));

        assertEquals(List.of("R_BASE", "R_PARA"), codes(result));
        assertTrue(result.points().get(0).foundation());
        assertFalse(result.points().get(1).foundation());
    }

    @Test
    void everyPrerequisiteComesBeforeThePointsThatNeedIt() {
        Point c = reader.add("R_C", "C", null, "C1");
        Point b = reader.add("R_B", "B", null, "C1");
        Point a = reader.add("R_A", "A", null, "C1");
        reader.prerequisites.put(c.id(), Set.of(b.id()));
        reader.prerequisites.put(b.id(), Set.of(a.id()));

        CourseCandidates result = selector.select(request(List.of(), List.of(c.id(), b.id(), a.id())));

        assertEquals(List.of("R_A", "R_B", "R_C"), codes(result));
    }

    @Test
    void thePointLimitHoldsAndAskedPointsGoFirst() {
        properties.getAi().setMaxCourseCandidates(3);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 6; i++) ids.add(reader.add("R_" + i, "Point " + i, null, "C1").id());
        Point foundation = reader.add("R_BASE", "Base", null, "B2");
        reader.prerequisites.put(ids.get(0), Set.of(foundation.id()));

        CourseCandidates result = selector.select(request(List.of(), ids));

        assertEquals(3, result.points().size());
        assertTrue(result.points().stream().noneMatch(CourseCandidates.Candidate::foundation),
                "foundation points only fill room the asked points leave");
    }

    @Test
    void eachPointCarriesItsPracticeSetsAndQuestions() {
        Point asked = reader.add("R_PARA", "Paraphrase", null, "C1");
        PracticeSetRef set = new PracticeSetRef(UUID.randomUUID(), "PS-1");
        UUID question = UUID.randomUUID();
        reader.sets.put(asked.id(), List.of(set));
        reader.questions.put(asked.id(), List.of(question));

        CourseCandidates.Candidate candidate = selector.select(request(List.of(), List.of(asked.id()))).points().get(0);

        assertEquals(List.of(set), candidate.practiceSets());
        assertEquals(List.of(question), candidate.questions());
    }

    @Test
    void namesAreNormalisedForMatching() {
        assertEquals("true false not given", CourseCandidateSelector.normalize("True/False/Not Given"));
        assertEquals("ngu phap", CourseCandidateSelector.normalize("  Ngữ   pháp "));
        assertEquals("", CourseCandidateSelector.normalize("***"));
    }
}
