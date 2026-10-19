package com.ieltspath.community.application.port;

import com.ieltspath.community.application.result.StoredMediaResult;

public interface CommunityStoragePort {

    StoredMediaResult storeImage(String key, byte[] content, String contentType);

    String publicUrl(String key);

    void delete(String key);
}

