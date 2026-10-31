package com.ieltspath.content.application.result;

public record StoredMediaResult(
        String key,
        String url,
        String contentType,
        long size
) {
}
