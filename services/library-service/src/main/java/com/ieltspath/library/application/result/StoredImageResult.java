package com.ieltspath.library.application.result;

public record StoredImageResult(
        String key,
        String url,
        String contentType,
        long size
) {
}
