package com.ieltspath.commonstorage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Lỗi hạ tầng của lưu trữ object.
 *
 * <p>Là {@link RuntimeException} thuần, không phải {@link IllegalArgumentException}: các
 * {@code GlobalExceptionHandler} của service map {@code IllegalArgumentException} thành HTTP 400, còn lỗi
 * hạ tầng phải đi đường 500.
 *
 * <p>Thông báo cố định, chỉ gồm thao tác, lý do, mã HTTP và {@link #ref}. Không chứa key, bucket, header,
 * body, message của Supabase hay nguyên nhân gốc. Không gắn {@code cause}, vì message của các exception
 * HTTP chứa URL đầy đủ có {@code userId}.
 */
public class StorageException extends RuntimeException {

    /** Lý do lỗi, cố định. */
    public enum Reason {
        BAD_REQUEST,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        ALREADY_EXISTS,
        PAYLOAD_TOO_LARGE,
        RATE_LIMITED,
        UPSTREAM_ERROR,
        UNAVAILABLE,
        MISCONFIGURED,
        UNEXPECTED_RESPONSE
    }

    private final String operation;
    private final Reason reason;
    private final int status;
    private final boolean outcomeUnknown;
    private final String ref;

    public StorageException(String operation, Reason reason, int status, boolean outcomeUnknown, String ref) {
        super(format(operation, reason, status, outcomeUnknown, ref));
        this.operation = operation;
        this.reason = reason;
        this.status = status;
        this.outcomeUnknown = outcomeUnknown;
        this.ref = ref;
    }

    /** Lỗi không có {@code bucket/key} cụ thể (ví dụ cấu hình chưa đủ). */
    public static StorageException withoutObject(String operation, Reason reason) {
        return new StorageException(operation, reason, 0, false, "-");
    }

    /**
     * 8 ký tự hex đầu của SHA-256 của {@code bucket + "/" + key}. Đủ để đối chiếu log với một object cụ thể,
     * không lộ key (key chứa {@code userId}).
     */
    public static String ref(String bucket, String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((bucket + "/" + key).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 4);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK", e);
        }
    }

    public String operation() {
        return operation;
    }

    public Reason reason() {
        return reason;
    }

    public int status() {
        return status;
    }

    /** true khi timeout hoặc lỗi I/O xảy ra giữa chừng: object có thể đã được ghi hoặc xóa. */
    public boolean outcomeUnknown() {
        return outcomeUnknown;
    }

    public String ref() {
        return ref;
    }

    private static String format(String operation, Reason reason, int status, boolean outcomeUnknown, String ref) {
        return "storage " + operation + " failed: reason=" + reason + " status=" + status
                + " outcomeUnknown=" + outcomeUnknown + " ref=" + ref;
    }
}
