package com.ieltspath.commonstorage;

import java.time.Duration;

/**
 * Cổng duy nhất để service ghi, xóa và cấp URL cho object lưu trữ.
 *
 * <p>Mọi phương thức nhận {@code bucket} làm tham số: module không biết bucket nào công khai hay riêng tư.
 * Ghi, xóa và kiểm tra tồn tại đều gọi mạng; {@link #publicUrl} thì không.
 */
public interface ObjectStorage {

    /**
     * Ghi object mới. Không ghi đè.
     *
     * <p>Chỉ dùng cho file nhỏ, vì nội dung được giữ trong bộ nhớ. File lớn (mp3 của admin) dùng
     * {@link #signedUploadUrl}. Sau {@link StorageException} có {@code outcomeUnknown=true}, không retry cùng key:
     * sinh key mới và xóa key cũ (best-effort).
     *
     * @throws StorageException {@code ALREADY_EXISTS} nếu key đã tồn tại
     */
    StoredObject put(String bucket, String key, byte[] content, String contentType);

    /** Xóa object. Idempotent: key không tồn tại không phải lỗi. */
    void delete(String bucket, String key);

    /** Object có tồn tại không. Gọi trước khi tin một key do client gửi lại. */
    boolean exists(String bucket, String key);

    /** Không gọi mạng. Chỉ đúng với bucket công khai. */
    String publicUrl(String bucket, String key);

    /**
     * URL tải có hạn cho bucket riêng tư, thời hạn trong khoảng [1 giây, 15 phút].
     * URL là bí mật: không ghi vào log.
     */
    String signedDownloadUrl(String bucket, String key, Duration ttl);

    /**
     * URL để client tự {@code PUT} file. Thời hạn do Supabase quyết định, không truyền được.
     * Đường này không đi qua kiểm tra nội dung của module.
     */
    SignedUpload signedUploadUrl(String bucket, String key);
}
