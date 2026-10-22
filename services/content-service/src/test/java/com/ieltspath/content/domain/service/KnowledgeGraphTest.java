package com.ieltspath.content.domain.service;

import com.ieltspath.content.domain.exception.PrerequisiteCycleException;
import com.ieltspath.content.domain.vo.CefrLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Test
    void prerequisiteAtTheSameOrALowerLevelDoesNotConflict() {
        UUID point = UUID.randomUUID();
        UUID same = UUID.randomUUID();
        UUID lower = UUID.randomUUID();

        assertThat(graph.levelConflicts(point, CefrLevel.B2,
                List.of(new KnowledgeGraph.Edge(same, point), new KnowledgeGraph.Edge(lower, point)),
                Map.of(same, CefrLevel.B2, lower, CefrLevel.A2))).isEmpty();
    }

    @Test
    void prerequisiteAboveThePointConflicts() {
        UUID point = UUID.randomUUID();
        UUID higher = UUID.randomUUID();

        assertThat(graph.levelConflicts(point, CefrLevel.B1,
                List.of(new KnowledgeGraph.Edge(higher, point)), Map.of(higher, CefrLevel.C1)))
                .containsExactly(higher);
    }

    @Test
    void dependentBelowThePointConflicts() {
        UUID point = UUID.randomUUID();
        UUID dependent = UUID.randomUUID();

        assertThat(graph.levelConflicts(point, CefrLevel.C1,
                List.of(new KnowledgeGraph.Edge(point, dependent)), Map.of(dependent, CefrLevel.B2)))
                .containsExactly(dependent);
    }

    @Test
    void neighbourWithoutALevelNeverConflicts() {
        UUID point = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        Map<UUID, CefrLevel> levels = new HashMap<>();
        levels.put(unknown, null);

        assertThat(graph.levelConflicts(point, CefrLevel.B1,
                List.of(new KnowledgeGraph.Edge(unknown, point)), levels)).isEmpty();
        assertThat(graph.levelConflicts(point, null,
                List.of(new KnowledgeGraph.Edge(unknown, point)), Map.of(unknown, CefrLevel.C2))).isEmpty();
    }

    @Test
    void levelViolationsListOnlyEdgesRunningFromAHigherLevelToALowerOne() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        KnowledgeGraph.Edge ok = new KnowledgeGraph.Edge(a, b);
        KnowledgeGraph.Edge inverted = new KnowledgeGraph.Edge(c, b);

        assertThat(graph.levelViolations(List.of(ok, inverted),
                Map.of(a, CefrLevel.B1, b, CefrLevel.B2, c, CefrLevel.C1))).containsExactly(inverted);
    }

    @Test
    void isolatedNodesHaveNoEdgeAsPrerequisiteOrDependent() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID alone = UUID.randomUUID();

        assertThat(graph.isolated(List.of(a, b, alone), List.of(new KnowledgeGraph.Edge(a, b))))
                .containsExactly(alone);
    }
}
