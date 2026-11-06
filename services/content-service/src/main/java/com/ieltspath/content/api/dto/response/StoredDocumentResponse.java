package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.StoredMediaResult;

/** An uploaded lesson document. It has no public URL: the key is attached to a lesson resource, which signs links. */
public record StoredDocumentResponse(String mediaReference, String contentType, long sizeBytes) {
    public static StoredDocumentResponse from(StoredMediaResult result) {
        return new StoredDocumentResponse(result.key(), result.contentType(), result.size());
    }
}
