package com.ieltspath.content.domain.service;

import com.ieltspath.content.domain.exception.PrerequisiteCycleException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeGraphTest {
    private final KnowledgeGraph graph = new KnowledgeGraph();

    @Test
    void reportsCycleInTraversalOrder() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        assertThatThrownBy(() -> graph.ensureAcyclic(List.of(
                new KnowledgeGraph.Edge(a, b),
                new KnowledgeGraph.Edge(b, c),
                new KnowledgeGraph.Edge(c, a))))
                .isInstanceOfSatisfying(PrerequisiteCycleException.class,
                        exception -> assertThat(exception.getCycle()).containsExactly(a, b, c, a));
    }

    @Test
    void rejectsSelfReferentialPrerequisite() {
        UUID a = UUID.randomUUID();

        assertThatThrownBy(() -> graph.ensureAcyclic(List.of(new KnowledgeGraph.Edge(a, a))))
                .isInstanceOfSatisfying(PrerequisiteCycleException.class,
                        exception -> assertThat(exception.getCycle()).containsExactly(a, a));
    }

    @Test
    void acceptsEmptyGraph() {
        assertThatNoException().isThrownBy(() -> graph.ensureAcyclic(List.of()));
    }

    @Test
    void acceptsDiamondWithoutReportingFalseCycle() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();

        assertThatNoException().isThrownBy(() -> graph.ensureAcyclic(List.of(
                new KnowledgeGraph.Edge(a, b),
                new KnowledgeGraph.Edge(a, c),
                new KnowledgeGraph.Edge(b, d),
                new KnowledgeGraph.Edge(c, d))));
    }

    @Test
    void handlesThreeHundredNodeChainWithoutRecursion() {
        List<KnowledgeGraph.Edge> chain = new ArrayList<>();
        UUID previous = UUID.randomUUID();
        for (int index = 1; index < 300; index++) {
            UUID next = UUID.randomUUID();
            chain.add(new KnowledgeGraph.Edge(previous, next));
            previous = next;
        }

        assertThatNoException().isThrownBy(() -> graph.ensureAcyclic(chain));
    }
}
