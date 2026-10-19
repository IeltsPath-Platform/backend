package com.ieltspath.community.application.result;

public record StoredMediaResult(
        String url,
        String key,
        String contentType,
        long size
) {
}

