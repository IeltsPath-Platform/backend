package com.ieltspath.commonllm;

import java.time.Duration;
import java.util.Objects;

/** Connection settings supplied by the calling service; credentials are never rendered. */
public record LlmEndpoint(String baseUrl, String apiKey, String model, String reasoningEffort,
                          Duration timeout, int maxTokens) {
    public LlmEndpoint {
        baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        apiKey = apiKey == null ? "" : apiKey;
        model = model == null ? "" : model;
        Objects.requireNonNull(timeout);
        if (timeout.isZero() || timeout.isNegative() || maxTokens <= 0)
            throw new IllegalArgumentException("LLM timeout and token budget must be positive");
    }

    @Override public String toString() { return "LlmEndpoint[model=" + model + "]"; }
}
