package com.ieltspath.commonresilience;

public class DownstreamUnavailableException extends RuntimeException {
    private final String target;
    private final String reason;

    public DownstreamUnavailableException(String target, String reason, Throwable cause) {
        super("Downstream '" + target + "' is unavailable (" + reason + ")", cause);
        this.target = target;
        this.reason = reason;
    }

    public String target() { return target; }
    public String reason() { return reason; }
}
