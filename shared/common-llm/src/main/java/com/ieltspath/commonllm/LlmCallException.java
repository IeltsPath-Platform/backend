package com.ieltspath.commonllm;

/** Safe failure metadata only; never retains the provider exception or response body. */
public class LlmCallException extends RuntimeException {
    private final String code;
    private final String reason;

    public LlmCallException(String code) { this(code, null); }
    public LlmCallException(String code, String reason) {
        super(code);
        this.code = code;
        this.reason = reason;
    }
    public String code() { return code; }
    public String reason() { return reason; }
}
