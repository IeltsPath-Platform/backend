package com.ieltspath.learning.application.result;

import com.ieltspath.learning.application.result.LearnerReportResult.Action;
import com.ieltspath.learning.application.result.LearnerReportResult.KnowledgePointReport;
import com.ieltspath.learning.application.result.LearnerReportResult.Score;
import com.ieltspath.learning.application.usecase.GetLearnerTreeUseCase.Node;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorTrapNestingContractTest {
    @Test
    void reportParentsAndKnowledgeTreeNodesExposeChildTrapArrays() {
        UUID parent = UUID.randomUUID();
        KnowledgePointReport report = new KnowledgePointReport(parent, "PARENT", "Parent", "READING", null,
                new Score(.8, "MAINTAIN", 10), List.of(), List.of(), new Action("NONE", null, null));
        Node node = new Node(parent, "PARENT", "Parent", "READING", "PROCEDURE", null, null,
                .8, "MAINTAIN", 10, null, List.of(), true);

        assertThat(report.errorTraps()).isEmpty();
        assertThat(node.errorTraps()).isEmpty();
    }
}
