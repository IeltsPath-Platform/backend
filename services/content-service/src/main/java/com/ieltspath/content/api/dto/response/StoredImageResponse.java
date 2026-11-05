package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.StoredMediaResult;

public record StoredImageResponse(String mediaReference, String mediaUrl, String contentType, long sizeBytes) {
    public static StoredImageResponse from(StoredMediaResult result) {
        return new StoredImageResponse(result.key(), result.url(), result.contentType(), result.size());
    }
}
