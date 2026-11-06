package com.ieltspath.learning.application.port;

import com.ieltspath.learning.domain.vo.SpeechAnalysis;

/**
 * Turns a recording into a transcript and pronunciation scores. The adapter is the only code that knows which speech
 * service does it; failures are thrown as {@link com.ieltspath.learning.application.exception.SpeechUnavailableException}.
 */
public interface SpeechAnalysisClient {
    /** Whether the service is configured; false lets the caller refuse before any work is queued. */
    boolean available();

    /** {@code wav} is 16 kHz mono PCM; {@code locale} is such as {@code en-US}. */
    SpeechAnalysis analyze(byte[] wav, String locale);
}
