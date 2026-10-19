package com.ieltspath.commonstorage;

/** Kết quả ghi thành công một object. */
public record StoredObject(String bucket, String key, String contentType, long sizeBytes) {
}
