package com.ieltspath.content.domain.service;

import com.ieltspath.content.domain.exception.PrerequisiteCycleException;

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
}
