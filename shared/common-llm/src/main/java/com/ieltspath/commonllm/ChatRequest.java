package com.ieltspath.commonllm;

/** Technical chat options. The caller owns prompt semantics and purpose names. */
public record ChatRequest(String purpose, String systemPrompt, String userPrompt,
                          boolean jsonMode, Integer maxTokensOverride) {
    public ChatRequest {
        if (purpose == null || purpose.isBlank()) throw new IllegalArgumentException("LLM purpose is required");
        if (maxTokensOverride != null && maxTokensOverride <= 0)
            throw new IllegalArgumentException("LLM token budget must be positive");
    }
    @Override public String toString() { return "ChatRequest[purpose=" + purpose + ", jsonMode=" + jsonMode + "]"; }
}
