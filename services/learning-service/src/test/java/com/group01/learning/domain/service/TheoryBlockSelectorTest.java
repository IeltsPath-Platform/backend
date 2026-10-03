package com.group01.learning.domain.service;

import com.group01.learning.domain.service.TheoryBlockSelector.BlockView;
import com.group01.learning.domain.service.TheoryBlockSelector.QuestionView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TheoryBlockSelectorTest {
    private final TheoryBlockSelector selector = new TheoryBlockSelector();
    private final UUID kpA = new UUID(0, 1);
    private final UUID kpB = new UUID(0, 2);

    private static UUID id(int n) { return new UUID(1, n); }

    private static QuestionView question(int n, UUID kp) { return new QuestionView(id(100 + n), n, List.of(kp)); }

    @Test
    void choosesTheKnowledgePointsTextAndAtMostThreeGradedQuestionsInLessonOrder() {
        List<BlockView> blocks = List.of(
                new BlockView(id(4), 4, false, true, List.of(kpA), List.of(question(3, kpA), question(1, kpA))),
                new BlockView(id(2), 2, true, false, List.of(kpB), List.of()),
                new BlockView(id(1), 1, true, false, List.of(kpA), List.of()),
                new BlockView(id(3), 3, false, true, List.of(kpA, kpB), List.of(question(2, kpB), question(5, kpA))),
                new BlockView(id(5), 5, false, false, List.of(kpA), List.of(question(6, kpA))),
                new BlockView(id(6), 6, false, true, List.of(kpA), List.of(question(7, kpA))));

        var selection = selector.select(blocks, kpA);

        assertThat(selection.theoryBlockIds()).containsExactly(id(1));
        assertThat(selection.knowledgePointScoped()).isTrue();
        // Block 3 first, then block 4 by question order; the essay (block 5) and the fourth question are left out.
        assertThat(selection.quickCheckQuestionIds()).containsExactly(id(105), id(101), id(103));
        assertThat(selector.select(blocks, kpA)).isEqualTo(selection);
    }

    @Test
    void fallsBackToEveryTextBlockWhenNoneTeachesTheKnowledgePoint() {
        List<BlockView> blocks = List.of(
                new BlockView(id(2), 2, true, false, List.of(), List.of()),
                new BlockView(id(1), 1, true, false, List.of(kpB), List.of()));

        var selection = selector.select(blocks, kpA);

        assertThat(selection.theoryBlockIds()).containsExactly(id(1), id(2));
        assertThat(selection.knowledgePointScoped()).isFalse();
        assertThat(selection.quickCheckQuestionIds()).isEmpty();
    }
}
