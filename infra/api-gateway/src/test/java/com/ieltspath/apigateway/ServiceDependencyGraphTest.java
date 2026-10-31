package com.ieltspath.apigateway;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synchronous calls between services must run one way. The graph is read from the service URLs each service
 * declares in the shared config; calls that are not declared there (and events) are not seen by this test.
 */
class ServiceDependencyGraphTest {
    /**
     * Edges that still close a cycle and are scheduled to be removed. Each one must still exist: when the code that
     * needs it is gone, this test fails until the entry is deleted, so the list cannot go stale.
     */
    private static final Set<String> KNOWN_CYCLE_EDGES = Set.of(
            "user-service->learning-service",
            "user-service->assessment-service");

    @Test
    void detectsACycleAndNamesIt() {
        Optional<List<String>> cycle = ServiceDependencyGraph.findCycle(Map.of(
                "a", Set.of("b"), "b", Set.of("c"), "c", Set.of("a")));
        assertTrue(cycle.isPresent());
        assertEquals(4, cycle.get().size());
        assertEquals(cycle.get().getFirst(), cycle.get().getLast());
        assertTrue(cycle.get().containsAll(List.of("a", "b", "c")));
    }

    @Test
    void acceptsADependencyGraphWithoutCycles() {
        assertFalse(ServiceDependencyGraph.findCycle(Map.of(
                "a", Set.of("b", "c"), "b", Set.of("c"), "c", Set.of())).isPresent());
    }

    @Test
    void detectsTwoServicesCallingEachOther() {
        assertTrue(ServiceDependencyGraph.findCycle(Map.of("a", Set.of("b"), "b", Set.of("a"))).isPresent());
    }

    @Test
    void sharedConfigDeclaresNoCallCycleExceptTheKnownOnes() throws IOException {
        Map<String, Set<String>> graph = ServiceDependencyGraph.fromConfigRepo(
                ServiceUrlWiringConfigTest.repoRoot().resolve("infra/config-server/config-repo"));
        Set<String> declared = new TreeSet<>();
        graph.forEach((from, targets) -> targets.forEach(to -> declared.add(from + "->" + to)));

        assertTrue(declared.containsAll(KNOWN_CYCLE_EDGES),
                "A known cycle edge no longer exists; remove it from KNOWN_CYCLE_EDGES. Declared: " + declared);

        Map<String, Set<String>> withoutKnown = ServiceDependencyGraph.without(graph, KNOWN_CYCLE_EDGES);
        Optional<List<String>> cycle = ServiceDependencyGraph.findCycle(withoutKnown);
        assertFalse(cycle.isPresent(), () -> "Services call each other in a cycle: " + String.join(" -> ", cycle.get()));
    }

    @Test
    void theKnownEdgesReallyCloseCyclesSoTheGuardWorksOnTheRealConfig() throws IOException {
        Map<String, Set<String>> graph = ServiceDependencyGraph.fromConfigRepo(
                ServiceUrlWiringConfigTest.repoRoot().resolve("infra/config-server/config-repo"));
        assertTrue(ServiceDependencyGraph.findCycle(graph).isPresent(),
                "With the known edges the real config has cycles; if not, delete KNOWN_CYCLE_EDGES and its test");
    }
}
