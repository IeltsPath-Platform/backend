package com.ieltspath.access.domain.exception;

/**
 * A key of a batch that nobody has handed out yet. To the learner it looks exactly like a code that does not exist, so
 * the answer never says that the key is real; the handler maps it to the not-found response.
 */
public class KeyNotIssuedException extends RuntimeException {

    public KeyNotIssuedException() {
        super("Mã kích hoạt không hợp lệ");
    }
}
