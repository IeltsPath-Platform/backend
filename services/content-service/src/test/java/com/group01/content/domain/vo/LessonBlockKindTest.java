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

    private static QuestionSpec essay(String passBand) {
        return new QuestionSpec("ESSAY", passBand);
    }

    @Test
    void autoGradedQuestionsMakeAnExerciseBlock() {
        assertThat(LessonBlockKind.classify(List.of(new QuestionSpec("CHOICE", null), new QuestionSpec("FILL", null),
                new QuestionSpec(null, null)))).isEqualTo(LessonBlockKind.EXERCISE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"6", "6.0", "4.0", "9", "7.5"})
    void oneValidEssayMakesAnEssayBlock(String passBand) {
        assertThat(LessonBlockKind.classify(List.of(essay(passBand)))).isEqualTo(LessonBlockKind.ESSAY);
    }

    @Test
    void essayMixedWithOtherQuestionsOrTwoEssaysIsRejected() {
        assertThatThrownBy(() -> LessonBlockKind.classify(List.of(essay("6.0"), new QuestionSpec("CHOICE", null))))
                .isInstanceOf(InvalidLessonBlockException.class);
        assertThatThrownBy(() -> LessonBlockKind.classify(List.of(essay("6.0"), essay("6.0"))))
                .isInstanceOf(InvalidLessonBlockException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"6.3", "9.5", "3.5", "abc", " "})
    void essayWithInvalidPassBandIsRejected(String passBand) {
        assertThatThrownBy(() -> LessonBlockKind.classify(List.of(essay(passBand))))
                .isInstanceOf(InvalidLessonBlockException.class);
    }

    @Test
    void essayWithoutPassBandIsRejected() {
        assertThatThrownBy(() -> LessonBlockKind.classify(List.of(essay(null))))
                .isInstanceOf(InvalidLessonBlockException.class);
    }
}
