package com.ieltspath.content.application.port;

/**
 * A failed generation. The code is safe to store and show; the message never carries the prompt or the reply.
 */
public class ContentGenerationException extends RuntimeException {

    public static final String NOT_CONFIGURED = "NOT_CONFIGURED";
    public static final String FAILED = "FAILED";
    public static final String INVALID_REPLY = "INVALID_REPLY";
    /** A course request matched no knowledge point, so there is nothing to plan from. */
    public static final String NO_CANDIDATES = "NO_CANDIDATES";

    private final String code;

    public ContentGenerationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
