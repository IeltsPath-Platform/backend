package com.ieltspath.learning.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.exception.SpeechUnavailableException;
import com.ieltspath.learning.application.port.SpeechAnalysisClient;
import com.ieltspath.learning.application.service.WavFormat;
import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.microsoft.cognitiveservices.speech.CancellationErrorCode;
import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.OutputFormat;
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig;
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGradingSystem;
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGranularity;
import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Analyses one recording with Azure Speech in a single continuous run, without a reference text: the transcript and the
 * pronunciation scores come back together, for a recording of any length. The recording is pushed through a stream, so
 * nothing with the learner's voice is written to disk. Every failure is a {@link SpeechUnavailableException}; the codes
 * name the cause and the {@code retryable} flag says whether trying again can help. Retrying is the caller's job.
 *
 * <p>This is not wrapped in {@code ResilientCall}: that guards HTTP calls made through Spring, and the SDK opens its own
 * connection. What it needs instead is a cap on analyses at once ({@code maxConcurrent}), because the service limits
 * concurrent requests.
 */
@Component
public class AzureSpeechClient implements SpeechAnalysisClient {
    private static final Logger log = LoggerFactory.getLogger(AzureSpeechClient.class);
    /** Causes that will not change on a second try: the key is refused or the request itself is wrong. */
    private static final Set<CancellationErrorCode> PERMANENT = Set.of(CancellationErrorCode.AuthenticationFailure,
            CancellationErrorCode.BadRequest, CancellationErrorCode.Forbidden);

    private final AzureSpeechSettings settings;
    private final AzureSpeechResultParser parser;
    private final Semaphore slots;

    @Autowired
    public AzureSpeechClient(AzureSpeechSettings settings, ObjectMapper json) {
        this(settings, new AzureSpeechResultParser(json));
    }

    AzureSpeechClient(AzureSpeechSettings settings, AzureSpeechResultParser parser) {
        this.settings = settings;
        this.parser = parser;
        this.slots = new Semaphore(settings.maxConcurrent());
    }

    @Override
    public boolean available() {
        return settings.configured();
    }

    @Override
    public SpeechAnalysis analyze(byte[] wav, String locale) {
        if (!available()) throw new SpeechUnavailableException("SPEECH_NOT_CONFIGURED", false);
        byte[] pcm;
        try {
            pcm = WavFormat.pcm(wav);
        } catch (IllegalArgumentException unsuitable) {
            throw new SpeechUnavailableException("RECORDING_FORMAT_UNSUPPORTED", false);
        }
        String language = locale == null || locale.isBlank() ? settings.locale() : locale;
        acquireSlot();
        try {
            return parser.parse("[" + String.join(",", recognize(pcm, language)) + "]");
        } finally {
            slots.release();
        }
    }

    private void acquireSlot() {
        try {
            if (!slots.tryAcquire(settings.timeoutSeconds(), TimeUnit.SECONDS)) {
                throw new SpeechUnavailableException("SPEECH_BUSY", true);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new SpeechUnavailableException("SPEECH_INTERRUPTED", true);
        }
    }

    /** The detailed JSON of each stretch of speech, in order. */
    private List<String> recognize(byte[] pcm, String language) {
        List<String> results = new CopyOnWriteArrayList<>();
        AtomicReference<SpeechUnavailableException> failure = new AtomicReference<>();
        CountDownLatch finished = new CountDownLatch(1);
        int timeout = settings.timeoutSeconds();

        try (SpeechConfig config = SpeechConfig.fromSubscription(settings.key(), settings.region())) {
            config.setSpeechRecognitionLanguage(language);
            config.setOutputFormat(OutputFormat.Detailed);
            config.requestWordLevelTimestamps();
            // An empty reference text asks the service to judge what it heard, so no script is needed.
            // Phoneme granularity returns the score of every sound of every word. IPA symbols are what a learner or a
            // teacher reads; the three best candidates for each sound tell which sound was said when it was not the right one.
            PronunciationAssessmentConfig assessment = new PronunciationAssessmentConfig("",
                    PronunciationAssessmentGradingSystem.HundredMark, PronunciationAssessmentGranularity.Phoneme, false);
            assessment.setPhonemeAlphabet("IPA");
            assessment.setNBestPhonemeCount(3);
            if ("en-US".equalsIgnoreCase(language)) assessment.enableProsodyAssessment();

            try (PushAudioInputStream stream = AudioInputStream.createPushStream(
                         AudioStreamFormat.getWaveFormatPCM(WavFormat.SAMPLE_RATE, (short) 16, (short) 1));
                 AudioConfig audio = AudioConfig.fromStreamInput(stream);
                 SpeechRecognizer recognizer = new SpeechRecognizer(config, audio)) {
                assessment.applyTo(recognizer);
                recognizer.recognized.addEventListener((sender, event) -> {
                    if (event.getResult().getReason() == ResultReason.RecognizedSpeech) {
                        results.add(event.getResult().getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult));
                    }
                });
                recognizer.canceled.addEventListener((sender, event) -> {
                    if (event.getReason() == CancellationReason.Error) failure.set(failureOf(event.getErrorCode()));
                    finished.countDown();
                });
                recognizer.sessionStopped.addEventListener((sender, event) -> finished.countDown());

                recognizer.startContinuousRecognitionAsync().get(timeout, TimeUnit.SECONDS);
                stream.write(pcm);
                stream.close();
                if (!finished.await(timeout, TimeUnit.SECONDS)) throw new SpeechUnavailableException("SPEECH_TIMEOUT", true);
                recognizer.stopContinuousRecognitionAsync().get(timeout, TimeUnit.SECONDS);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new SpeechUnavailableException("SPEECH_INTERRUPTED", true);
        } catch (TimeoutException timedOut) {
            throw new SpeechUnavailableException("SPEECH_TIMEOUT", true);
        } catch (ExecutionException | RuntimeException exception) {
            if (exception instanceof SpeechUnavailableException unavailable) throw unavailable;
            // The message of an SDK error can carry detail of the request; only its type is logged.
            log.warn("Speech analysis failed: {}", exception.getClass().getSimpleName());
            throw new SpeechUnavailableException("SPEECH_FAILED", true);
        }
        if (failure.get() != null) throw failure.get();
        return results;
    }

    private SpeechUnavailableException failureOf(CancellationErrorCode code) {
        log.warn("Speech analysis refused: code={}", code);
        return PERMANENT.contains(code) ? new SpeechUnavailableException("SPEECH_REJECTED_" + code.name(), false)
                : new SpeechUnavailableException("SPEECH_UNAVAILABLE_" + code.name(), true);
    }
}
