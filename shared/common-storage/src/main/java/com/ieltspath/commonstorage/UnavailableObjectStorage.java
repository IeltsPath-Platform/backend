package com.ieltspath.commonstorage;

import java.time.Duration;

/**
 * Bản cài khi bật storage mà chưa cấp {@code SUPABASE_URL} và {@code SUPABASE_SECRET_KEY}.
 *
 * <p>Service vẫn khởi động. Chỉ tính năng dùng storage trả lỗi {@code UNAVAILABLE}. Lý do: compose đặt
 * {@code restart: unless-stopped}, nên một biến thiếu không được làm sập cả service (ví dụ đăng nhập).
 */
public final class UnavailableObjectStorage implements ObjectStorage {

    private static StorageException unavailable(String operation) {
        return StorageException.withoutObject(operation, StorageException.Reason.UNAVAILABLE);
    }

    @Override
    public StoredObject put(String bucket, String key, byte[] content, String contentType) {
        throw unavailable("put");
    }

    @Override
    public void delete(String bucket, String key) {
        throw unavailable("delete");
    }

    @Override
    public boolean exists(String bucket, String key) {
        throw unavailable("exists");
    }

    @Override
    public String publicUrl(String bucket, String key) {
        throw unavailable("publicUrl");
    }

    @Override
    public String signedDownloadUrl(String bucket, String key, Duration ttl) {
        throw unavailable("signDownload");
    }

    @Override
    public SignedUpload signedUploadUrl(String bucket, String key) {
        throw unavailable("signUpload");
    }
}
