package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.StoredImageResult;

public record FlashcardImageResponse(
        String key,
        String url,
        String contentType,
        long size
) {
    public static FlashcardImageResponse from(StoredImageResult result) {
        return new FlashcardImageResponse(result.key(), result.url(), result.contentType(), result.size());
    }
}
