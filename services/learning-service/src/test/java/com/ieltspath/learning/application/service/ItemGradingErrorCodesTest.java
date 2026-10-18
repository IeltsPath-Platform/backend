package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ItemGradingErrorCodesTest {
    private final UUID choiceId = UUID.randomUUID();
    private final UUID fillId = UUID.randomUUID();

    @Test
    void wrongObjectiveAnswersCarryTheirErrorTagAndCorrectOnesDoNot() {
        Item choice = new Item(choiceId, 1, "Pick one", null, Map.of("type", "CHOICE", "correct", "A",
                "distractors", Map.of("B", "PARAPHRASE_TRAP")), "Because", BigDecimal.ONE, List.of());
        Item fill = new Item(fillId, 2, "Fill it", null, Map.of("type", "FILL", "accepted", List.of("critics")),
                "Plural", BigDecimal.ONE, List.of());
        PackageVersion version = new PackageVersion(UUID.randomUUID(), UUID.randomUUID(), "PRACTICE_SET", null,
                Map.of(), List.of(new Section(UUID.randomUUID(), "Section", "READING", null, 1, null,
                        Arrays.asList(choice, fill))));

        var graded = new ItemGrading().grade(version, List.of(
                new SubmitExerciseCommand.Answer(choiceId, "B"),
                new SubmitExerciseCommand.Answer(fillId, "critic")));

        assertThat(graded.errorCodes()).containsExactly("PARAPHRASE_TRAP", "PLURAL_FORM");
        assertThat(graded.correct()).isZero();
    }

    @Test
    void correctAnswersHaveNoErrorTag() {
        Item choice = new Item(choiceId, 1, "Pick one", null, Map.of("type", "CHOICE", "correct", "A",
                "distractors", Map.of("B", "PARAPHRASE_TRAP")), "Because", BigDecimal.ONE, List.of());
        PackageVersion version = new PackageVersion(UUID.randomUUID(), UUID.randomUUID(), "PRACTICE_SET", null,
                Map.of(), List.of(new Section(UUID.randomUUID(), "Section", "READING", null, 1, null,
                        List.of(choice))));

        var graded = new ItemGrading().grade(version, List.of(new SubmitExerciseCommand.Answer(choiceId, "A")));

        assertThat(graded.errorCodes()).containsExactly((String) null);
    }
}
