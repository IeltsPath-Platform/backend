package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.GenerationReply;

/** The only way the content service talks to a language model. Tests replace it; nothing else calls a model. */
public interface ContentGenerationPort {
    /** Sends one prompt and returns the model's text. Throws {@link ContentGenerationException} on any failure. */
    GenerationReply generate(String prompt);
}
