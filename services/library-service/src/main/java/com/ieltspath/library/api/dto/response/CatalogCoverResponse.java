package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.StoredImageResult;

public record CatalogCoverResponse(String mediaReference, String mediaUrl, String contentType, long sizeBytes) {
    public static CatalogCoverResponse from(StoredImageResult result) {
        return new CatalogCoverResponse(result.key(), result.url(), result.contentType(), result.size());
    }
}
