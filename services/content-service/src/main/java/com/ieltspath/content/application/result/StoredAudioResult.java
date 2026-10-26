package com.ieltspath.content.application.result;

public record StoredAudioResult(
        String key,
        String url,
        String contentType,
        long size
) {
}
