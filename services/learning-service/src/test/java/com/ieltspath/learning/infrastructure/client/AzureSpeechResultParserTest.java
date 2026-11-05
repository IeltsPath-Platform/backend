package com.ieltspath.learning.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.exception.SpeechUnavailableException;
import com.ieltspath.learning.domain.service.SpeakingRhythm;
import com.ieltspath.learning.domain.service.WeakWords;
import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.SpokenWord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the parser and the measures built on it over two real Azure results: a native English voice reading 62 words, and
 * a Vietnamese voice reading English with a heavy accent (205 words, 12 segments).
 */
class AzureSpeechResultParserTest {
    private final AzureSpeechResultParser parser = new AzureSpeechResultParser(new ObjectMapper());

    private static String resource(String name) throws IOException {
        try (var input = AzureSpeechResultParserTest.class.getResourceAsStream("/speech/" + name)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void readsTheTranscriptAndWordTimesOfANativeVoice() throws IOException {
        SpeechAnalysis analysis = parser.parse(resource("azure-native-voice.json"));

        assertEquals(5, analysis.segments().size());
        assertEquals(62, analysis.words().size());
        assertTrue(analysis.transcript().startsWith("I come from a small town near the sea."), analysis.transcript());
        assertTrue(analysis.transcript().endsWith("it is very rewarding."), analysis.transcript());
        // Azure counts in 100 ns: 500000 is 50 ms, and the second word starts at 270 ms.
        assertEquals(50, analysis.words().get(0).startMs());
        assertEquals(270, analysis.words().get(1).startMs());
        assertEquals(210, analysis.words().get(0).durationMs());
        assertFalse(analysis.isEmpty());
    }

    @Test
    void theWholeRecordingScoreIsTheSegmentsWeightedByTheirWords() throws IOException {
        SpeechAnalysis analysis = parser.parse(resource("azure-native-voice.json"));

        // The figures the spike printed for the same file.
        assertEquals(94.0, analysis.accuracy(), 0.05);
        assertEquals(97.4, analysis.fluency(), 0.05);
        assertEquals(92.7, analysis.pronunciation(), 0.05);
        assertEquals(90.6, analysis.prosody(), 0.05);
    }

    @Test
    void measuresTheRhythmOfARecordingFromItsWordTimes() throws IOException {
        SpeakingRhythm rhythm = SpeakingRhythm.of(parser.parse(resource("azure-native-voice.json")));

        assertEquals(62, rhythm.wordCount());
        // About 24 seconds of speech for 62 words: a steady reading, not a rushed or a halting one.
        assertTrue(rhythm.speakingTimeMs() > 20_000 && rhythm.speakingTimeMs() < 30_000, "time " + rhythm.speakingTimeMs());
        assertTrue(rhythm.wordsPerMinute() > 110 && rhythm.wordsPerMinute() < 190, "wpm " + rhythm.wordsPerMinute());
        // Azure set the recording apart into five stretches at its pauses, so there are pauses between them.
        assertTrue(rhythm.longPauses() >= 3, "pauses " + rhythm.longPauses());
        assertTrue(rhythm.longestPauseMs() >= 700, "longest " + rhythm.longestPauseMs());
        assertTrue(rhythm.pauseRatio() > 0 && rhythm.pauseRatio() < 0.5, "ratio " + rhythm.pauseRatio());
    }

    @Test
    void aHeavilyAccentedReadingScoresLowerAndHasWeakWordsToPractise() throws IOException {
        SpeechAnalysis native_ = parser.parse(resource("azure-native-voice.json"));
        SpeechAnalysis accented = parser.parse(resource("azure-heavy-accent-voice.json"));

        // 205 words in Azure's own list; the display text has 202 blanks-separated tokens because Azure split one
        // misheard token ("AT1JE") into three words.
        assertEquals(205, accented.words().size());
        assertEquals(12, accented.segments().size());
        assertTrue(accented.pronunciation() < native_.pronunciation());

        List<SpokenWord> weak = WeakWords.select(accented, 8);
        assertFalse(weak.isEmpty());
        assertTrue(weak.size() <= 8);
        for (int index = 1; index < weak.size(); index++) {
            assertTrue(weak.get(index - 1).accuracy() <= weak.get(index).accuracy(), "worst first");
        }
        assertEquals(weak.size(), weak.stream().map(word -> word.text().toLowerCase()).distinct().count());
        assertTrue(weak.stream().allMatch(word -> word.accuracy() < WeakWords.WEAK_BELOW || word.hasError()));
    }

    @Test
    void aSegmentThatWasNotSpeechIsSkipped() {
        String results = """
                [{"RecognitionStatus":"NoMatch","Offset":0,"Duration":1000000},
                 {"RecognitionStatus":"Success","DisplayText":"Hello there.","NBest":[{"PronunciationAssessment":
                   {"AccuracyScore":80,"FluencyScore":90,"CompletenessScore":100,"PronScore":85},
                   "Words":[{"Word":"hello","Offset":0,"Duration":3000000,"PronunciationAssessment":{"AccuracyScore":80,"ErrorType":"None"}}]}]}]
                """;

        SpeechAnalysis analysis = parser.parse(results);

        assertEquals(1, analysis.segments().size());
        assertEquals("Hello there.", analysis.transcript());
    }

    @Test
    void aSegmentWithoutProsodyHasNoProsodyScore() {
        String results = """
                {"RecognitionStatus":"Success","DisplayText":"Hi.","NBest":[{"PronunciationAssessment":
                  {"AccuracyScore":80,"FluencyScore":90,"CompletenessScore":100,"PronScore":85},"Words":[]}]}
                """;

        SpeechAnalysis analysis = parser.parse(results);

        assertEquals(1, analysis.segments().size());
        assertNull(analysis.segments().get(0).prosody());
        assertNull(analysis.prosody());
    }

    @Test
    void speechThatWasNotHeardGivesAnEmptyAnalysisNotAnError() {
        SpeechAnalysis analysis = parser.parse("[{\"RecognitionStatus\":\"InitialSilenceTimeout\"}]");

        assertTrue(analysis.isEmpty());
        assertEquals("", analysis.transcript());
        assertEquals(0, analysis.pronunciation());
    }

    @Test
    void textThatIsNotJsonIsAPermanentFailure() {
        SpeechUnavailableException failure = assertThrows(SpeechUnavailableException.class, () -> parser.parse("not json {"));

        assertFalse(failure.isRetryable());
    }

    @Test
    void readsTheSoundsOfEachWordAndWhatWasHeardInsteadOfAWeakOne() {
        String results = """
                {"RecognitionStatus":"Success","DisplayText":"Think.","NBest":[{"PronunciationAssessment":
                  {"AccuracyScore":70,"FluencyScore":90,"CompletenessScore":100,"PronScore":75},
                  "Words":[{"Word":"think","Offset":0,"Duration":3000000,"PronunciationAssessment":{"AccuracyScore":52,"ErrorType":"Mispronunciation"},
                    "Phonemes":[
                      {"Phoneme":"θ","PronunciationAssessment":{"AccuracyScore":25,"NBestPhonemes":[{"Phoneme":"s","Score":71},{"Phoneme":"θ","Score":25}]}},
                      {"Phoneme":"ɪ","PronunciationAssessment":{"AccuracyScore":90,"NBestPhonemes":[{"Phoneme":"ɪ","Score":90},{"Phoneme":"i","Score":40}]}},
                      {"Phoneme":"ŋ","PronunciationAssessment":{"AccuracyScore":95}}]}]}]}
                """;

        List<SpokenWord> words = parser.parse(results).words();

        assertEquals(1, words.size());
        var sounds = words.get(0).phonemes();
        assertEquals(List.of("θ", "ɪ", "ŋ"), sounds.stream().map(sound -> sound.symbol()).toList());
        assertEquals(25.0, sounds.get(0).accuracy());
        assertEquals("s", sounds.get(0).heardAs());      // the best match was another sound
        assertNull(sounds.get(1).heardAs());             // the expected sound was the best match
        assertNull(sounds.get(2).heardAs());             // no candidates given
        assertEquals(List.of("θ"), words.get(0).weakPhonemes().stream().map(sound -> sound.symbol()).toList());
    }

    @Test
    void aWordScoredOnlyAtWordLevelHasNoSounds() throws IOException {
        SpeechAnalysis analysis = parser.parse(resource("azure-native-voice.json"));

        assertTrue(analysis.words().stream().allMatch(word -> word.phonemes().isEmpty()));
    }

    @Test
    void aStoredWordWithoutSoundsStillReadsBackAsEmpty() throws Exception {
        SpokenWord word = new ObjectMapper().readValue(
                "{\"text\":\"a\",\"startMs\":0,\"durationMs\":1,\"accuracy\":50,\"errorType\":\"None\"}", SpokenWord.class);

        assertTrue(word.phonemes().isEmpty());
    }

    @Test
    void wordsWithSoundsSurviveBeingStoredAndReadBack() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SpokenWord word = new SpokenWord("think", 0, 300, 52, "None", List.of(new com.ieltspath.learning.domain.vo.SpokenPhoneme("θ", 25, "s")));

        assertEquals(word, mapper.readValue(mapper.writeValueAsString(word), SpokenWord.class));
    }

    @Test
    void writesTheEnglishRAsALearnerKnowsIt() {
        String results = """
                {"RecognitionStatus":"Success","DisplayText":"Angry.","NBest":[{"PronunciationAssessment":
                  {"AccuracyScore":70,"FluencyScore":90,"CompletenessScore":100,"PronScore":75},
                  "Words":[{"Word":"angry","Offset":0,"Duration":3000000,"PronunciationAssessment":{"AccuracyScore":52,"ErrorType":"None"},
                    "Phonemes":[
                      {"Phoneme":"ɹ","PronunciationAssessment":{"AccuracyScore":23,"NBestPhonemes":[{"Phoneme":"w","Score":60},{"Phoneme":"ɹ","Score":23}]}},
                      {"Phoneme":"ɡ","PronunciationAssessment":{"AccuracyScore":22,"NBestPhonemes":[{"Phoneme":"v","Score":50}]}},
                      {"Phoneme":"ɚ","PronunciationAssessment":{"AccuracyScore":90}}]}]}]}
                """;

        var sounds = parser.parse(results).words().get(0).phonemes();

        assertEquals(List.of("r", "g", "ər"), sounds.stream().map(sound -> sound.symbol()).toList());
        assertEquals("w", sounds.get(0).heardAs());
        assertEquals("v", sounds.get(1).heardAs());
    }
}
