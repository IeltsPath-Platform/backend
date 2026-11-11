package com.ieltspath.commonllm;

/** Provider text and optional usage. The caller parses the text according to its own contract. */
public record ChatReply(String content, Long inputTokens, Long outputTokens, String model) {
    @Override public String toString() { return "ChatReply[model=" + model + "]"; }
}
