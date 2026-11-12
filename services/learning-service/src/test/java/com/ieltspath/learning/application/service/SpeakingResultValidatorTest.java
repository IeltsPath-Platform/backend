package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.service.SpeakingResultValidator.InvalidSpeakingGradeException;
import com.ieltspath.learning.application.service.SpeakingResultValidator.ValidatedGrade;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeakingResultValidatorTest {
    private final SpeakingResultValidator validator = new SpeakingResultValidator(new ObjectMapper());
    private final SpeakingGradingInput input = SpeakingTestData.input(SpeakingScope.PART, new int[]{1},
            SpeakingTestData.analysis(SpeakingTestData.SENTENCE, 80, 0.95));
    private final Set<String> kps = Set.of("S65_GRAMMAR_RANGE");

    @Test
    void keepsTheThreeBandsAndDropsWhatItCannotCheck() {
        ValidatedGrade grade = validator.validate(SpeakingTestData.goodAnswer("6.0", "6.5", "5.5"), input, kps);

        assertThat(grade.criteria()).extracting("code").containsExactly("FC", "LR", "GRA");
        assertThat(grade.criteria()).extracting("band")
                .containsExactly(new BigDecimal("6.0"), new BigDecimal("6.5"), new BigDecimal("5.5"));
        assertThat(grade.criteria().get(0).evidence()).hasSize(1);
        assertThat(grade.criteria().get(0).evidence().get(0).quote()).isEqualTo("my teacher was very kind");
        assertThat(grade.errors()).extracting("code").containsExactly("SPK_NO_LINKER");
        assertThat(grade.kpObservations()).extracting("kpCode").containsExactly("S65_GRAMMAR_RANGE");
        assertThat(grade.reliability().level()).isEqualTo("HIGH");
    }

    @Test
    void keepsAThoroughListOfErrorsUpToTheLimitAndNoFurther() {
        String error = "{\"code\":\"SPK_NO_LINKER\",\"partNo\":1,\"excerpt\":\"she taught me English\",\"suggestion\":\"add because\"}";
        String many = SpeakingTestData.goodAnswer("6.0", "6.0", "6.0").replaceFirst("\"errors\":\\[[^\\]]*\\]",
                "\"errors\":[" + String.join(",", java.util.Collections.nCopies(SpeakingResultValidator.MAX_ERRORS + 10, error)) + "]");

        assertThat(validator.validate(many, input, kps).errors()).hasSize(SpeakingResultValidator.MAX_ERRORS);
        assertThat(SpeakingResultValidator.MAX_ERRORS).isGreaterThan(10);
    }

    @Test
    void keepsMoreThanFourWeaknessesAndFiveEvidenceEntriesPerCriterion() {
        String quote = "{\"quote\":\"my teacher was very kind\",\"note\":\"clear\"}";
        String answer = SpeakingTestData.goodAnswer("6.0", "6.0", "6.0")
                .replace("\"weaknesses\":[\"b\"]", "\"weaknesses\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\"]")
                .replaceFirst("\"evidence\":\\[[^\\]]*\\]", "\"evidence\":[" + String.join(",", java.util.Collections.nCopies(10, quote)) + "]");

        var first = validator.validate(answer, input, kps).criteria().get(0);

        assertThat(first.weaknesses()).hasSize(SpeakingResultValidator.MAX_POINTS);
        assertThat(first.evidence()).hasSize(SpeakingResultValidator.MAX_EVIDENCE);
    }

    @Test
    void aWeaknessKeepsRoomForTheCountTheQuotedPlacesAndTheirFixes() {
        String longWeakness = "x".repeat(650);
        String answer = SpeakingTestData.goodAnswer("6.0", "6.0", "6.0")
                .replace("\"weaknesses\":[\"b\"]", "\"weaknesses\":[\"" + longWeakness + "\"]");

        var first = validator.validate(answer, input, kps).criteria().get(0);

        assertThat(first.weaknesses()).containsExactly(longWeakness);
        assertThat(SpeakingResultValidator.MAX_WEAKNESS_CHARS).isGreaterThan(300);
    }

    @Test
    void dropsObservationsThatNameAKnowledgePointThatWasNotOffered() {
        ValidatedGrade grade = validator.validate(SpeakingTestData.goodAnswer("6.0", "6.0", "6.0"), input, Set.of());

        assertThat(grade.kpObservations()).isEmpty();
    }

    @Test
    void aMissingBandIsNeverReadAsZero() {
        String missing = SpeakingTestData.goodAnswer("6.0", "6.0", "6.0").replace("\"code\":\"LR\",\"band\":6.0,", "\"code\":\"LR\",");

        assertThatThrownBy(() -> validator.validate(missing, input, kps)).isInstanceOf(InvalidSpeakingGradeException.class);
    }

    @Test
    void aBandThatIsNotANumberOrNotOnTheScaleIsRefused() {
        for (String bad : new String[]{"\"six\"", "\"6.0\"", "6.3", "9.5", "-1", "null"}) {
            assertThatThrownBy(() -> validator.validate(SpeakingTestData.goodAnswer(bad, "6.0", "6.0"), input, kps))
                    .as(bad).isInstanceOf(InvalidSpeakingGradeException.class);
        }
    }

    @Test
    void aMissingCriterionOrAnAnswerThatIsNotAnObjectIsRefused() {
        assertThatThrownBy(() -> validator.validate("{\"criteria\":[{\"code\":\"FC\",\"band\":6}]}", input, kps))
                .isInstanceOf(InvalidSpeakingGradeException.class);
        assertThatThrownBy(() -> validator.validate("not json", input, kps)).isInstanceOf(InvalidSpeakingGradeException.class);
        assertThatThrownBy(() -> validator.validate("[1,2]", input, kps)).isInstanceOf(InvalidSpeakingGradeException.class);
    }

    @Test
    void anItemNamingAPartThatWasNotAnsweredIsDropped() {
        String answer = SpeakingTestData.goodAnswer("6.0", "6.0", "6.0").replace("\"partNo\":1", "\"partNo\":3");

        assertThat(validator.validate(answer, input, kps).errors()).isEmpty();
    }

    @Test
    void textForDisplayLosesMarkupAndLinks() {
        String answer = SpeakingTestData.goodAnswer("6.0", "6.0", "6.0").replace("\"summary\":\"Good.\"",
                "\"summary\":\"<b>Good</b> see https://evil.example now\"");

        assertThat(validator.validate(answer, input, kps).summary()).isEqualTo("Good see now");
    }

    @Test
    void aPronunciationNoteIsKeptOnlyForAWordAndASoundTheSpeechServiceListed() {
        var withSounds = SpeakingTestData.input(SpeakingScope.PART, new int[]{1}, SpeakingTestData.analysisWithSounds());
        String notes = """
                ,"pronunciationNotes":[
                  {"word":"think","sound":"θ","tip":"Đặt đầu lưỡi giữa hai hàm răng rồi thổi hơi."},
                  {"word":"Think","sound":"θ","tip":"Trùng lặp nên bị bỏ."},
                  {"word":"banana","sound":"b","tip":"Từ không có trong danh sách."},
                  {"word":"thanks","sound":"z","tip":"Âm này không thuộc từ thanks."},
                  {"word":"thanks","sound":"","tip":"Luyện cả từ chậm rồi nhanh dần."},
                  {"word":"think","sound":"ŋ","tip":""}]""";
        String answer = SpeakingTestData.goodAnswer("6.0", "6.0", "6.0").replaceFirst("\\}\\s*$", java.util.regex.Matcher.quoteReplacement(notes) + "}");

        var kept = validator.validate(answer, withSounds, kps).pronunciationNotes();

        assertThat(kept).extracting("word", "sound").containsExactly(org.assertj.core.groups.Tuple.tuple("think", "θ"),
                org.assertj.core.groups.Tuple.tuple("thanks", ""));
    }

    @Test
    void withoutSoundScoresNoPronunciationNoteIsKept() {
        String notes = ",\"pronunciationNotes\":[{\"word\":\"think\",\"sound\":\"θ\",\"tip\":\"Một mẹo.\"}]";
        String answer = SpeakingTestData.goodAnswer("6.0", "6.0", "6.0").replaceFirst("\\}\\s*$", notes + "}");

        assertThat(validator.validate(answer, input, kps).pronunciationNotes()).isEmpty();
    }}
