package com.ieltspath.apigateway;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Who calls whom, read from the service URLs declared in the shared config, and a search for call cycles. */
final class ServiceDependencyGraph {
    /** Environment variables such as CONTENT_SERVICE_URL name the service a config file reaches out to. */
    private static final Pattern SERVICE_URL = Pattern.compile("\\$\\{([A-Z]+)_SERVICE_URL\\b");

    private ServiceDependencyGraph() {}

    /** One entry per {@code *-service.yaml}: the services its URL variables point at, other than itself. */
    static Map<String, Set<String>> fromConfigRepo(Path configRepo) throws IOException {
        Map<String, Set<String>> graph = new TreeMap<>();
        try (Stream<Path> files = Files.list(configRepo)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith("-service.yaml")).toList()) {
                String caller = file.getFileName().toString().replace(".yaml", "");
                Set<String> targets = new TreeSet<>();
                Matcher matcher = SERVICE_URL.matcher(Files.readString(file));
                while (matcher.find()) {
                    String callee = matcher.group(1).toLowerCase() + "-service";
                    if (!callee.equals(caller)) targets.add(callee);
                }
                graph.put(caller, targets);
            }
        }
        return graph;
    }

    /** A cycle as a path whose first and last service are the same, or empty when calls only run one way. */
    static Optional<List<String>> findCycle(Map<String, Set<String>> graph) {
        Set<String> finished = new HashSet<>();
        for (String start : new TreeSet<>(graph.keySet())) {
            Optional<List<String>> cycle = visit(start, graph, finished, new ArrayDeque<>(), new HashSet<>());
            if (cycle.isPresent()) return cycle;
        }
        return Optional.empty();
    }

    private static Optional<List<String>> visit(String node, Map<String, Set<String>> graph, Set<String> finished,
                                                Deque<String> path, Set<String> onPath) {
        if (finished.contains(node)) return Optional.empty();
        path.addLast(node);
        onPath.add(node);
        for (String next : new TreeSet<>(graph.getOrDefault(node, Collections.emptySet()))) {
            if (onPath.contains(next)) {
                List<String> cycle = new ArrayList<>(path);
                cycle = new ArrayList<>(cycle.subList(cycle.indexOf(next), cycle.size()));
                cycle.add(next);
                return Optional.of(cycle);
            }
            Optional<List<String>> found = visit(next, graph, finished, path, onPath);
            if (found.isPresent()) return found;
        }
        path.removeLast();
        onPath.remove(node);
        finished.add(node);
        return Optional.empty();
    }
}
