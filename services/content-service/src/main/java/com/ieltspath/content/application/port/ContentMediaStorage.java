package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.StoredMediaResult;

public interface ContentMediaStorage {
    StoredMediaResult store(String key, byte[] content, String contentType);
}
