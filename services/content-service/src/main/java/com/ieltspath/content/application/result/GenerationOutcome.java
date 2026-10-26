package com.ieltspath.content.application.result;

/** What a generator hands back for a draft: the JSON result and the versions that produced it. */
public record GenerationOutcome(String resultJson, String model, String promptVersion) {
}
