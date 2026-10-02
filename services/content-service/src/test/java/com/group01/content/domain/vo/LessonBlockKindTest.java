package com.group01.content.domain.vo;

import com.group01.content.domain.exception.InvalidLessonBlockException;
import com.group01.content.domain.vo.LessonBlockKind.QuestionSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonBlockKindTest {

    private static QuestionSpec task2(String passBand) {
        return new QuestionSpec("ESSAY", "TASK_2", passBand, null, List.of());
    }

    private static QuestionSpec task1(String chartFacts, List<AssetType> assets) {
        return new QuestionSpec("ESSAY", "TASK_1", "6.0", chartFacts, assets);
    }

    private static QuestionSpec objective(String type) {
        return new QuestionSpec(type, null, null, null, List.of());
    }

    private static void assertRejected(QuestionSpec... questions) {
        assertThatThrownBy(() -> LessonBlockKind.classify(List.of(questions)))
                .isInstanceOf(InvalidLessonBlockException.class);
    }

    @Test
    void autoGradedQuestionsMakeAnExerciseBlock() {
        assertThat(LessonBlockKind.classify(List.of(objective("CHOICE"), objective("FILL"), objective(null))))
                .isEqualTo(LessonBlockKind.EXERCISE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"6", "6.0", "4.0", "9", "7.5"})
    void oneValidTask2EssayMakesAnEssayBlockWithoutImages(String passBand) {
        assertThat(LessonBlockKind.classify(List.of(task2(passBand)))).isEqualTo(LessonBlockKind.ESSAY);
    }

    @Test
    void task1EssayWithChartFactsAndAnImageMakesAnEssayBlock() {
        assertThat(LessonBlockKind.classify(List.of(task1("Madrid: black roof 82.", List.of(AssetType.IMAGE)))))
                .isEqualTo(LessonBlockKind.ESSAY);
    }

    @Test
    void task1EssayWithoutChartFactsOrImageIsRejected() {
        assertRejected(task1(null, List.of(AssetType.IMAGE)));
        assertRejected(task1(" ", List.of(AssetType.IMAGE)));
        assertRejected(task1("x".repeat(2001), List.of(AssetType.IMAGE)));
        assertRejected(task1("Madrid: black roof 82.", List.of()));
        assertRejected(task1("Madrid: black roof 82.", List.of(AssetType.AUDIO)));
    }

    @Test
    void essayMixedWithOtherQuestionsTwoEssaysOrUnknownTaskIsRejected() {
        assertRejected(task2("6.0"), objective("CHOICE"));
        assertRejected(task2("6.0"), task2("6.0"));
        assertRejected(new QuestionSpec("ESSAY", "TASK_3", "6.0", null, List.of()));
        assertRejected(new QuestionSpec("ESSAY", null, "6.0", null, List.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"6.3", "9.5", "3.5", "abc", " "})
    void essayWithInvalidPassBandIsRejected(String passBand) {
        assertRejected(task2(passBand));
    }

    @Test
    void essayWithoutPassBandIsRejected() {
        assertRejected(task2(null));
    }
}
