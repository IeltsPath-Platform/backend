package com.ieltspath.learning.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.domain.service.PronunciationBandMapper;
import com.ieltspath.learning.domain.service.SpeakingAnalysisSummary;
import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Sends a real recording to Azure Speech. Skipped unless {@code LEARNING_SPEECH_KEY} and {@code LEARNING_SPEECH_REGION}
 * are set and {@code SPEECH_TEST_WAV} names a 16 kHz mono 16-bit WAV (keep it outside the repository: it is a person's
 * voice). It costs a little on the Azure bill, so it never runs in the normal build.
 */
@EnabledIfEnvironmentVariable(named = "LEARNING_SPEECH_KEY", matches = ".+")
class AzureSpeechClientLiveTest {

    @Test
    void aRefusedKeyIsAPermanentFailureNotOneToRetry() {
        byte[] silence = new byte[44 + 32_000];
        java.nio.ByteBuffer.wrap(silence).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".getBytes()).putInt(36 + 32_000).put("WAVE".getBytes()).put("fmt ".getBytes()).putInt(16)
                .putShort((short) 1).putShort((short) 1).putInt(16000).putInt(32000).putShort((short) 2).putShort((short) 16)
                .put("data".getBytes()).putInt(32_000);
        AzureSpeechClient client = new AzureSpeechClient(new AzureSpeechSettings("not-a-valid-key",
                System.getenv().getOrDefault("LEARNING_SPEECH_REGION", "eastasia"), "en-US", 60, 1), new ObjectMapper());

        com.ieltspath.learning.application.exception.SpeechUnavailableException failure =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.ieltspath.learning.application.exception.SpeechUnavailableException.class,
                        () -> client.analyze(silence, "en-US"));

        assertFalse(failure.isRetryable(), failure.getMessage());
        assertTrue(failure.getMessage().startsWith("SPEECH_REJECTED_"), failure.getMessage());
    }

    @Test
    void analysesARealRecordingEndToEnd() throws Exception {
        String file = System.getenv("SPEECH_TEST_WAV");
        assumeTrue(file != null && !file.isBlank(), "SPEECH_TEST_WAV is not set");
        AzureSpeechSettings settings = new AzureSpeechSettings(System.getenv("LEARNING_SPEECH_KEY"),
                System.getenv().getOrDefault("LEARNING_SPEECH_REGION", ""),
                System.getenv().getOrDefault("LEARNING_SPEECH_LOCALE", "en-US"), 180, 2);
        AzureSpeechClient client = new AzureSpeechClient(settings, new ObjectMapper());
        assertTrue(client.available());

        long started = System.nanoTime();
        SpeechAnalysis analysis = client.analyze(Files.readAllBytes(Path.of(file)), null);
        long millis = (System.nanoTime() - started) / 1_000_000;
        SpeakingAnalysisSummary summary = SpeakingAnalysisSummary.of(List.of(analysis), PronunciationBandMapper.INTERIM, 5);

        assertFalse(analysis.isEmpty(), "nothing was recognised");
        assertFalse(analysis.transcript().isBlank());
        assertNotNull(summary.pronunciationBand());
        // Phoneme granularity: every word carries the scores of its sounds, in IPA.
        assertTrue(analysis.words().stream().anyMatch(word -> !word.phonemes().isEmpty()), "no sounds were returned");
        analysis.words().stream().filter(word -> !word.phonemes().isEmpty()).limit(4).forEach(word ->
                System.out.printf("live word %s %.0f: %s%n", word.text(), word.accuracy(), word.phonemes()));
        System.out.printf("live: %d segments, %d words, %d ms; pronunciation %.1f -> band %s; rhythm %.0f wpm, %d long pauses; "
                        + "weak words %d%n", analysis.segments().size(), analysis.words().size(), millis,
                summary.pronunciation(), summary.pronunciationBand(), summary.rhythm().wordsPerMinute(),
                summary.rhythm().longPauses(), summary.weakWords().size());
    }
}
