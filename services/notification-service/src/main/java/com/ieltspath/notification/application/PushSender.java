package com.ieltspath.notification.application;

import java.util.Map;

/** Sends one push message to one device token through the push provider. */
public interface PushSender {
    /**
     * @return the provider's message id
     * @throws PushSendException when the provider did not accept the message
     */
    String send(String token, String title, String body, Map<String, String> data);

    /** Why a push was not accepted; decides whether to retry and whether the device is dropped. */
    enum Failure { TOKEN_INVALID, PERMANENT, TRANSIENT }

    class PushSendException extends RuntimeException {
        private final Failure failure;
        private final String code;

        public PushSendException(Failure failure, String code) {
            super(code);
            this.failure = failure;
            this.code = code;
        }

        public Failure failure() { return failure; }

        public String code() { return code; }
    }
}
