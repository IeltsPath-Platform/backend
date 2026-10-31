package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.StoredMediaResult;

public record StoredAudioResponse(String mediaReference, String mediaUrl, String contentType, long sizeBytes) {
    public static StoredAudioResponse from(StoredMediaResult result) {
        return new StoredAudioResponse(result.key(), result.url(), result.contentType(), result.size());
    }
}
