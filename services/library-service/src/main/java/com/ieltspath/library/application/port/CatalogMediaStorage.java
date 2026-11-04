package com.ieltspath.library.application.port;

import com.ieltspath.library.application.result.StoredImageResult;

/** Public media of the vocabulary catalog: sense pictures and pronunciation audio, in the shared media bucket. */
public interface CatalogMediaStorage {
    StoredImageResult store(String key, byte[] content, String contentType);
}
