package com.ieltspath.content.domain.service;

import com.ieltspath.content.domain.exception.PrerequisiteCycleException;
import com.ieltspath.content.domain.vo.CefrLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Domain rules for the directed prerequisite graph. An edge points from prerequisite to dependent knowledge point. */
public class KnowledgeGraph {
    public record Edge(UUID from, UUID to) {}

    private record Frame(UUID node, Iterator<UUID> neighbors) {}

    public void ensureAcyclic(Collection<Edge> edges) {
        Map<UUID, Set<UUID>> adjacency = new LinkedHashMap<>();
        for (Edge edge : edges) {
            adjacency.computeIfAbsent(edge.from(), ignored -> new LinkedHashSet<>()).add(edge.to());
            adjacency.computeIfAbsent(edge.to(), ignored -> new LinkedHashSet<>());
        }

        Set<UUID> visited = new HashSet<>();
        Map<UUID, Integer> activePathIndex = new HashMap<>();
        List<UUID> activePath = new ArrayList<>();
        Deque<Frame> stack = new ArrayDeque<>();

        for (UUID start : adjacency.keySet()) {
            if (!visited.add(start)) {
                continue;
            }
            activePathIndex.put(start, 0);
            activePath.add(start);
            stack.push(new Frame(start, adjacency.get(start).iterator()));

            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                if (!frame.neighbors().hasNext()) {
                    stack.pop();
                    activePathIndex.remove(frame.node());
                    activePath.remove(activePath.size() - 1);
                    continue;
                }

                UUID next = frame.neighbors().next();
                Integer cycleStart = activePathIndex.get(next);
                if (cycleStart != null) {
                    List<UUID> cycle = new ArrayList<>(activePath.subList(cycleStart, activePath.size()));
                    cycle.add(next);
                    throw new PrerequisiteCycleException(cycle);
                }
                if (visited.add(next)) {
                    activePathIndex.put(next, activePath.size());
                    activePath.add(next);
                    stack.push(new Frame(next, adjacency.get(next).iterator()));
                }
            }
        }
    }

    /**
     * Neighbours of {@code pointId} whose CEFR level contradicts an edge touching it: a prerequisite above the point,
     * or a dependent below it. A prerequisite may sit at the same level. A neighbour with no level never conflicts.
     */
    public List<UUID> levelConflicts(UUID pointId, CefrLevel pointLevel, Collection<Edge> edges,
                                     Map<UUID, CefrLevel> levels) {
        if (pointLevel == null) {
            return List.of();
        }
        Set<UUID> conflicts = new LinkedHashSet<>();
        for (Edge edge : edges) {
            if (edge.to().equals(pointId)) {
                CefrLevel level = levels.get(edge.from());
                if (level != null && level.compareTo(pointLevel) > 0) {
                    conflicts.add(edge.from());
                }
            } else if (edge.from().equals(pointId)) {
                CefrLevel level = levels.get(edge.to());
                if (level != null && level.compareTo(pointLevel) < 0) {
                    conflicts.add(edge.to());
                }
            }
        }
        return List.copyOf(conflicts);
    }

    /** Edges whose prerequisite has a higher CEFR level than the point that depends on it. */
    public List<Edge> levelViolations(Collection<Edge> edges, Map<UUID, CefrLevel> levels) {
        List<Edge> violations = new ArrayList<>();
        for (Edge edge : edges) {
            CefrLevel from = levels.get(edge.from());
            CefrLevel to = levels.get(edge.to());
            if (from != null && to != null && from.compareTo(to) > 0) {
                violations.add(edge);
            }
        }
        return List.copyOf(violations);
    }

    /** Nodes that are neither a prerequisite nor a dependent in any edge. */
    public Set<UUID> isolated(Collection<UUID> nodeIds, Collection<Edge> edges) {
        Set<UUID> connected = new HashSet<>();
        for (Edge edge : edges) {
            connected.add(edge.from());
            connected.add(edge.to());
        }
        Set<UUID> isolated = new LinkedHashSet<>(nodeIds);
        isolated.removeAll(connected);
        return isolated;
    }
}
