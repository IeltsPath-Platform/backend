package com.ieltspath.content.application.result;

/** Text from the model and the model that produced it. */
public record GenerationReply(String text, String model) {
}
