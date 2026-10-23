package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.StoredAudioResult;

public record StoredAudioResponse(String mediaReference, String mediaUrl, String contentType, long sizeBytes) {
    public static StoredAudioResponse from(StoredAudioResult result) {
        return new StoredAudioResponse(result.key(), result.url(), result.contentType(), result.size());
    }
}
