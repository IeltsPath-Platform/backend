package com.ieltspath.community.api.dto.response;

import com.ieltspath.community.application.result.StoredMediaResult;

public record StoredMediaResponse(
        String url,
        String key,
        String contentType,
        long size
) {
    public static StoredMediaResponse from(StoredMediaResult result) {
        return new StoredMediaResponse(result.url(), result.key(), result.contentType(), result.size());
    }
}

