package com.ieltspath.apigateway;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synchronous calls between services must run one way. The graph is read from the service URLs each service
 * declares in the shared config; calls that are not declared there (and events) are not seen by this test.
 */
class ServiceDependencyGraphTest {
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
    void sharedConfigDeclaresNoCallCycle() throws IOException {
        Map<String, Set<String>> graph = ServiceDependencyGraph.fromConfigRepo(
                ServiceUrlWiringConfigTest.repoRoot().resolve("infra/config-server/config-repo"));
        assertFalse(graph.isEmpty(), "No service declares a *_SERVICE_URL; the test would pass without checking anything");

        Optional<List<String>> cycle = ServiceDependencyGraph.findCycle(graph);
        assertFalse(cycle.isPresent(), () -> "Services call each other in a cycle: " + String.join(" -> ", cycle.get()));
    }
}
