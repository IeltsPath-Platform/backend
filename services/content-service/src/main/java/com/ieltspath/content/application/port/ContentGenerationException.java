package com.ieltspath.content.application.port;

/**
 * A failed generation. The code is safe to store and show; the message never carries the prompt or the reply.
 */
public class ContentGenerationException extends RuntimeException {

    public static final String NOT_CONFIGURED = "NOT_CONFIGURED";
    /** The model call failed in a way that is none of the cases below (the endpoint could not be reached, say). */
    public static final String FAILED = "FAILED";
    /** The model did not answer within the configured time. */
    public static final String TIMEOUT = "TIMEOUT";
    /** The model answered with an error status: the code is this prefix and the status, e.g. {@code HTTP_429}. */
    public static final String HTTP_PREFIX = "HTTP_";
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
