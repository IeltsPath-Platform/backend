package com.group01.learning.application.port;

/** One OpenAI-compatible chat completion that is asked to answer with a JSON object. */
public interface LlmClient {
    /** False when no model, endpoint or API key is configured. */
    boolean configured();

    /**
     * @param lowReasoning ask the model to think less, so a retry spends its output budget on the JSON
     * @return the raw message text, which may still not be valid JSON
     * @throws com.group01.learning.application.exception.LlmUnavailableException on any provider or network failure
     */
    String completeJson(String systemPrompt, String userPrompt, boolean lowReasoning);
}
