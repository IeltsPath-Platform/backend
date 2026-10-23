package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.StoredAudioResult;

public interface ContentAudioStorage {
    StoredAudioResult store(String key, byte[] content, String contentType);
}
