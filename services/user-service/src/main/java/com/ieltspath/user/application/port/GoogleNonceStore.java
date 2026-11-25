package com.ieltspath.user.application.port;

public interface GoogleNonceStore {
    String issue();
    boolean consume(String nonce);
}
