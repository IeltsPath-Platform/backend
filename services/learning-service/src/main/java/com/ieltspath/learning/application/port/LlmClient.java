package com.ieltspath.learning.application.port;

/** One OpenAI-compatible chat completion that is asked to answer with a JSON object. */
public interface LlmClient {
    /**
     * How much room one call gets. A long structured answer (a Speaking grade) needs a larger output budget and a longer
     * wait than the default, which is sized for a short essay grade.
     *
     * @param maxTokens      output budget, including any tokens the model spends on thinking
     * @param timeoutSeconds how long to wait for the reply
     * @param retryTransient ask again, through the shared retry, when the provider answers 5xx or the connection fails.
     *                       A completion has no side effect, so repeating it is safe; a 429 is never asked again, because
     *                       it means the quota is gone. Off by default: only a call made of several requests at once, where
     *                       one failure spoils the whole result, turns it on (the Speaking grade).
     */
    record Budget(int maxTokens, int timeoutSeconds, boolean retryTransient) {
        public Budget(int maxTokens, int timeoutSeconds) {
            this(maxTokens, timeoutSeconds, false);
        }
    }

    /** False when no model, endpoint or API key is configured. */
    boolean configured();

    /**
     * @return the raw message text, which may still not be valid JSON
     * @throws com.ieltspath.learning.application.exception.LlmUnavailableException on any provider or network failure
     */
    String completeJson(String systemPrompt, String userPrompt);

    /** Same as {@link #completeJson(String, String)} with an explicit budget; a client that has no such knob ignores it. */
    default String completeJson(String systemPrompt, String userPrompt, Budget budget) {
        return completeJson(systemPrompt, userPrompt);
    }
}
