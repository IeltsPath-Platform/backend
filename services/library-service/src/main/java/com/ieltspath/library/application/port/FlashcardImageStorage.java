package com.ieltspath.library.application.port;

import com.ieltspath.library.application.result.StoredImageResult;

public interface FlashcardImageStorage {
    StoredImageResult store(String key, byte[] content, String contentType);

    String publicUrl(String key);

    boolean exists(String key);

    void delete(String key);

    void deleteAfterCommit(String key);
}
