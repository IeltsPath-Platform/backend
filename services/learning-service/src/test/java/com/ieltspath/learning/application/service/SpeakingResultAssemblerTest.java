package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.service.SpeakingResultAssembler.Assembled;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class SpeakingResultAssemblerTest {
    private static final String LONG_TEXT = String.join(" ", java.util.Collections.nCopies(3, SpeakingTestData.SENTENCE));
    private final SpeakingResultValidator validator = new SpeakingResultValidator(new ObjectMapper());
    private final SpeakingResultAssembler assembler = new SpeakingResultAssembler(SpeakingSettingsFixture.enabled());

    @Test
    void fullTestGetsAPronunciationBandFromTheTableAndAnOverallBandFromTheMean() {
        SpeechAnalysis analysis = SpeakingTestData.analysis(LONG_TEXT, 80, 0.95);
        Assembled assembled = assemble(SpeakingScope.FULL, new int[]{1, 2, 3}, analysis, "6.0", "6.0", "6.0");

        // accuracy 80 maps to band 6.5 in the interim table; (6 + 6 + 6 + 6.5) / 4 = 6.125 -> 6.0
        assertThat(criterion(assembled, "PR").get("band")).isEqualTo(new BigDecimal("6.5"));
        assertThat(assembled.overallBand()).isEqualByComparingTo("6.0");
        assertThat(display(assembled).get("overallBand")).isEqualTo(assembled.overallBand());
        assertThat(display(assembled).get("label")).isEqualTo("Điểm ước lượng để luyện tập");
    }

    @Test
    void anOverallBandIsRoundedUpFromAQuarterAndThreeQuarters() {
        SpeechAnalysis analysis = SpeakingTestData.analysis(LONG_TEXT, 80, 0.95);

        assertThat(assemble(SpeakingScope.FULL, new int[]{1, 2, 3}, analysis, "6.5", "6.5", "6.5").overallBand())
                .isEqualByComparingTo("6.5");
        assertThat(assemble(SpeakingScope.FULL, new int[]{1, 2, 3}, analysis, "7.0", "7.0", "7.0").overallBand())
                .isEqualByComparingTo("7.0"); // mean 6.875 rounds up to 7.0
    }

    @Test
    void thereIsNoOverallBandWithoutAllThreeParts() {
        SpeechAnalysis analysis = SpeakingTestData.analysis(LONG_TEXT, 80, 0.95);

        assertThat(assemble(SpeakingScope.PART, new int[]{2}, analysis, "6.0", "6.0", "6.0").overallBand()).isNull();
        assertThat(assemble(SpeakingScope.FULL, new int[]{1, 2}, analysis, "6.0", "6.0", "6.0").overallBand()).isNull();
    }

    @Test
    void aTooShortAnswerHasItsBandsCappedAndIsNotUsedAsEvidence() {
        SpeechAnalysis analysis = SpeakingTestData.analysis(SpeakingTestData.SENTENCE, 80, 0.95); // 15 words < 30
        Assembled assembled = assemble(SpeakingScope.PART, new int[]{1}, analysis, "7.0", "6.5", "6.0");

        assertThat(criterion(assembled, "FC").get("band")).isEqualTo(new BigDecimal("5.0"));
        assertThat(criterion(assembled, "FC").get("limited")).isEqualTo(true);
        assertThat(criterion(assembled, "GRA").get("band")).isEqualTo(new BigDecimal("5.0"));
        assertThat(signals(assembled).get("usableAsEvidence")).isEqualTo(false);
        assertThat(((List<Map<String, Object>>) display(assembled).get("bandLimits"))).extracting(item -> item.get("reason"))
                .containsExactly("TOO_SHORT");
    }

    @Test
    void anOffTopicAnswerIsCappedHarderThanAShortOne() {
        SpeechAnalysis analysis = SpeakingTestData.analysis(LONG_TEXT, 80, 0.95);
        String answer = SpeakingTestData.goodAnswer("7.0", "7.0", "7.0").replace("\"onTopic\":true", "\"onTopic\":false");
        Assembled assembled = assembler.assemble(SpeakingTestData.rubric(),
                SpeakingTestData.input(SpeakingScope.PART, new int[]{1}, analysis),
                validator.validate(answer, SpeakingTestData.input(SpeakingScope.PART, new int[]{1}, analysis), java.util.Set.of()));

        assertThat(criterion(assembled, "LR").get("band")).isEqualTo(new BigDecimal("4.0"));
    }

    @Test
    void signalsKeepKpObservationsUnappliedAndMapCriteriaToTheirKnowledgePoints() {
        SpeechAnalysis analysis = SpeakingTestData.analysis(LONG_TEXT, 80, 0.95);
        SpeakingGradingInput input = SpeakingTestData.input(SpeakingScope.PART, List.of(
                new SpeakingGradingInput.Answer(1, 1, "q", analysis)), analysis, SpeakingTestData.learnerWithGrammarPoint());
        Assembled assembled = assembler.assemble(SpeakingTestData.rubric(), input,
                validator.validate(SpeakingTestData.goodAnswer("6.0", "6.0", "5.5"), input, input.learner().allowedKpCodes()));
        Map<String, Object> signals = signals(assembled);

        Map<String, Object> observation = ((List<Map<String, Object>>) signals.get("kpObservations")).get(0);
        assertThat(observation).containsEntry("applied", false).containsEntry("excerptVerified", true)
                .containsEntry("kpId", SpeakingTestData.GRAMMAR_KP);
        assertThat((Map<String, Integer>) signals.get("errorCounts")).containsEntry("SPK_NO_LINKER", 1);
        List<Map<String, Object>> criteria = (List<Map<String, Object>>) signals.get("criteria");
        assertThat(criteria).filteredOn(row -> row.get("code").equals("FC")).singleElement()
                .satisfies(row -> assertThat(row.get("kpId")).isEqualTo(SpeakingTestData.FC_KP));
        assertThat(criteria).filteredOn(row -> row.get("code").equals("GRA")).singleElement()
                .satisfies(row -> assertThat(row.get("passed")).isEqualTo(false));
        assertThat(signals).containsEntry("signalsVersion", "sig1").containsEntry("usableAsEvidence", true);
    }

    @Test
    void theLearnerDisplayCarriesNoSignalsAndNoLearnerHistoryNumbers() {
        SpeechAnalysis analysis = SpeakingTestData.analysis(LONG_TEXT, 80, 0.95);
        Map<String, Object> display = display(assemble(SpeakingScope.PART, new int[]{1}, analysis, "6.0", "6.0", "6.0"));

        assertThat(display).doesNotContainKeys("kpObservations", "errorCounts", "signalsVersion");
        assertThat(((Map<String, Object>) display.get("personalFeedback")).get("note")).isNotNull();
    }

    private Assembled assemble(SpeakingScope scope, int[] parts, SpeechAnalysis analysis, String fc, String lr, String gra) {
        SpeakingGradingInput input = SpeakingTestData.input(scope, parts, analysis);
        return assembler.assemble(SpeakingTestData.rubric(), input,
                validator.validate(SpeakingTestData.goodAnswer(fc, lr, gra), input, java.util.Set.of()));
    }

    private static Map<String, Object> display(Assembled assembled) {
        return (Map<String, Object>) assembled.result().get("display");
    }

    private static Map<String, Object> signals(Assembled assembled) {
        return (Map<String, Object>) assembled.result().get("signals");
    }

    private static Map<String, Object> criterion(Assembled assembled, String code) {
        return ((List<Map<String, Object>>) display(assembled).get("criteria")).stream()
                .filter(item -> code.equals(item.get("code"))).findFirst().orElseThrow();
    }

    @Test
    void theDisplayShowsEveryWordAndSoundTheSpeechServiceScoredWithTheModelsTips() {
        SpeechAnalysis analysis = SpeakingTestData.analysisWithSounds();
        SpeakingGradingInput input = SpeakingTestData.input(SpeakingScope.PART, new int[]{1}, analysis);
        String notes = ",\"pronunciationNotes\":[{\"word\":\"think\",\"sound\":\"θ\",\"tip\":\"Đặt lưỡi giữa răng.\"}]";
        String answer = SpeakingTestData.goodAnswer("6.0", "6.0", "6.0").replaceFirst("\\}\\s*$", java.util.regex.Matcher.quoteReplacement(notes) + "}");
        Assembled assembled = assembler.assemble(SpeakingTestData.rubric(), input, validator.validate(answer, input, java.util.Set.of()));
        Map<String, Object> pronunciation = (Map<String, Object>) display(assembled).get("pronunciation");

        List<Map<String, Object>> weakWords = (List<Map<String, Object>>) pronunciation.get("weakWords");
        assertThat(weakWords).extracting(row -> row.get("word")).containsExactly("thanks", "think");
        Map<String, Object> think = weakWords.get(1);
        assertThat(think.get("tip")).isEqualTo("Đặt lưỡi giữa răng.");
        assertThat((List<Map<String, Object>>) think.get("sounds")).extracting(row -> row.get("symbol")).containsExactly("θ", "ɪ", "ŋ", "k");
        assertThat(((List<Map<String, Object>>) think.get("sounds")).get(0)).containsEntry("accuracy", 25.0).containsEntry("heardAs", "s");
        assertThat(weakWords.get(0).get("tip")).isNull();

        List<Map<String, Object>> weakSounds = (List<Map<String, Object>>) pronunciation.get("weakSounds");
        assertThat(weakSounds.get(0)).containsEntry("symbol", "θ").containsEntry("count", 2).containsEntry("heardAs", "s")
                .containsEntry("tip", "Đặt lưỡi giữa răng.");

        Map<String, Object> transcript = ((List<Map<String, Object>>) display(assembled).get("transcripts")).get(0);
        List<Map<String, Object>> words = (List<Map<String, Object>>) transcript.get("words");
        assertThat(words).extracting(row -> row.get("text")).containsExactly("I", "think", "my", "thanks");
        assertThat(words.get(1)).containsEntry("accuracy", 52.0).containsEntry("errorType", "Mispronunciation");
        assertThat(words.get(0).get("errorType")).isNull();
        assertThat((List<?>) signals(assembled).get("weakSounds")).isNotEmpty();
    }}
