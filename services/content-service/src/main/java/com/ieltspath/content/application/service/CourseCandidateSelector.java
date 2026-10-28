package com.ieltspath.content.application.service;

import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.application.port.CourseCandidateReader;
import com.ieltspath.content.application.port.CourseCandidateReader.Point;
import com.ieltspath.content.application.port.CourseCandidateReader.PracticeSetRef;
import com.ieltspath.content.application.result.CourseCandidates;
import com.ieltspath.content.application.result.CourseCandidates.Candidate;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Chooses, by fixed rules and without the model, the knowledge points a course draft may use: the ones asked for (by id
 * or by matching a topic phrase), the lower-level prerequisites that support them, direct and indirect, in teaching
 * order (the earliest prerequisite first), each with its
 * unlinked practice sets and learning questions. The model only ever sees and picks from this set.
 */
@Component
public class CourseCandidateSelector {

    static final int PRACTICE_SETS_PER_POINT = 5;
    static final int QUESTIONS_PER_POINT = 10;

    private final CourseCandidateReader reader;
    private final ContentAiProperties properties;

    public CourseCandidateSelector(CourseCandidateReader reader, ContentAiProperties properties) {
        this.reader = reader;
        this.properties = properties;
    }

    public CourseCandidates select(CourseDraftRequest request) {
        int limit = properties.getAi().getMaxCourseCandidates();
        CefrLevel level = CefrLevel.forBand(request.bandLevel());
        Set<String> skills = request.skills().stream().map(Skill::name).collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, Point> requested = new LinkedHashMap<>();
        reader.activeByIds(request.knowledgePointIds()).forEach(point -> requested.put(point.id(), point));

        List<String> unmatched = new ArrayList<>();
        if (!request.topics().isEmpty()) {
            List<Point> pool = reader.pool(level == null ? null : level.name(), skills);
            for (String phrase : request.topics()) {
                List<Point> matches = pool.stream().filter(point -> matches(point, phrase)).toList();
                if (matches.isEmpty()) {
                    unmatched.add(phrase);
                }
                matches.forEach(point -> requested.putIfAbsent(point.id(), point));
            }
        }

        List<Point> asked = requested.values().stream().limit(limit).toList();
        Map<UUID, Point> selected = new LinkedHashMap<>();
        asked.forEach(point -> selected.put(point.id(), point));
        Set<UUID> foundationIds = request.withPrerequisites() ? foundation(asked, level, limit - selected.size(), selected)
                : Set.of();
        reader.activeByIds(foundationIds).forEach(point -> selected.put(point.id(), point));

        List<UUID> ordered = teachingOrder(selected);
        Map<UUID, List<PracticeSetRef>> practiceSets = reader.practiceSets(ordered, PRACTICE_SETS_PER_POINT);
        Map<UUID, List<UUID>> questions = reader.exerciseQuestions(ordered, QUESTIONS_PER_POINT);
        Set<UUID> askedIds = asked.stream().map(Point::id).collect(Collectors.toSet());
        List<Candidate> candidates = ordered.stream().map(id -> {
            Point point = selected.get(id);
            return new Candidate(id, point.code(), point.name(), point.description(), !askedIds.contains(id),
                    practiceSets.getOrDefault(id, List.of()), questions.getOrDefault(id, List.of()));
        }).toList();
        return new CourseCandidates(candidates, unmatched);
    }

    /**
     * The prerequisites of the asked points, and theirs in turn, that sit at a lower CEFR level than the course and are
     * not already chosen. The graph is walked upward one step at a time, so a point near the asked ones is taken before
     * a deeper one when the room runs out. A point is looked at once, however many ways lead to it, and the walk goes on
     * only through the points it takes.
     */
    private Set<UUID> foundation(List<Point> asked, CefrLevel courseLevel, int room, Map<UUID, Point> selected) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (room <= 0 || courseLevel == null) return ids;
        Set<UUID> seen = new HashSet<>(selected.keySet());
        List<UUID> frontier = asked.stream().map(Point::id).toList();
        while (!frontier.isEmpty() && ids.size() < room) {
            Set<UUID> next = new LinkedHashSet<>();
            reader.prerequisites(frontier).values().forEach(next::addAll);
            next.removeAll(seen);
            seen.addAll(next);
            List<UUID> taken = new ArrayList<>();
            for (Point point : reader.activeByIds(next)) {
                CefrLevel pointLevel = level(point);
                if (pointLevel != null && pointLevel.compareTo(courseLevel) < 0 && ids.size() < room) {
                    ids.add(point.id());
                    taken.add(point.id());
                }
            }
            frontier = taken;
        }
        return ids;
    }

    /**
     * Kahn's algorithm over the prerequisite edges among the chosen points. When several points are ready at once the
     * one of the lowest CEFR level goes first, so the course climbs from the earliest prerequisite; after that, ties
     * keep the order they were chosen in.
     */
    private List<UUID> teachingOrder(Map<UUID, Point> selected) {
        List<UUID> chosen = List.copyOf(selected.keySet());
        Map<UUID, Integer> position = new HashMap<>();
        for (int i = 0; i < chosen.size(); i++) position.put(chosen.get(i), i);
        Map<UUID, Set<UUID>> prerequisites = reader.prerequisites(chosen);

        Map<UUID, Integer> waiting = new HashMap<>();
        Map<UUID, List<UUID>> dependents = new HashMap<>();
        for (UUID id : chosen) {
            Set<UUID> within = prerequisites.getOrDefault(id, Set.of()).stream()
                    .filter(position::containsKey).filter(other -> !other.equals(id)).collect(Collectors.toSet());
            waiting.put(id, within.size());
            within.forEach(prerequisite -> dependents.computeIfAbsent(prerequisite, ignored -> new ArrayList<>()).add(id));
        }
        PriorityQueue<UUID> ready = new PriorityQueue<>(java.util.Comparator
                .comparingInt((UUID id) -> levelRank(selected.get(id))).thenComparingInt(position::get));
        chosen.stream().filter(id -> waiting.get(id) == 0).forEach(ready::add);
        List<UUID> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            UUID next = ready.poll();
            ordered.add(next);
            for (UUID dependent : dependents.getOrDefault(next, Collections.emptyList())) {
                if (waiting.merge(dependent, -1, Integer::sum) == 0) ready.add(dependent);
            }
        }
        // A cycle cannot exist in a valid tree; if one does, the points left over still go in, in chosen order.
        chosen.stream().filter(id -> !ordered.contains(id)).forEach(ordered::add);
        return ordered;
    }

    /** A point without a level is taught after the ones that have one. */
    private static int levelRank(Point point) {
        CefrLevel level = level(point);
        return level == null ? CefrLevel.values().length : level.ordinal();
    }

    private static CefrLevel level(Point point) {
        try {
            return point.cefrLevel() == null ? null : CefrLevel.valueOf(point.cefrLevel());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** A phrase matches a point when each of its words appears in the point's name, code or description. */
    static boolean matches(Point point, String phrase) {
        String wanted = normalize(phrase);
        if (wanted.isEmpty()) return false;
        String haystack = normalize(point.name() + " " + point.code() + " " + (point.description() == null ? ""
                : point.description()));
        for (String word : wanted.split(" ")) {
            if (!haystack.contains(word)) return false;
        }
        return true;
    }

    static String normalize(String text) {
        if (text == null) return "";
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return plain.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
