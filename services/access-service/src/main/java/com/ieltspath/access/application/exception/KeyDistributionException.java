package com.ieltspath.access.application.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * A refusal in the key distribution flow that the screens act on: a stable {@code code}, the HTTP status, a sentence for
 * people and string details such as how many keys are left. Details never carry a key code.
 */
public class KeyDistributionException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, String> details;

    public KeyDistributionException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public KeyDistributionException(HttpStatus status, String code, String message, Map<String, String> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? Map.of() : details;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public Map<String, String> details() { return details; }

    public static KeyDistributionException notEnoughKeys(int available) {
        return new KeyDistributionException(HttpStatus.CONFLICT, "NOT_ENOUGH_KEYS",
                "Không đủ key sẵn sàng để thực hiện. Hiện còn " + available + " key.",
                Map.of("available", String.valueOf(available)));
    }

    public static KeyDistributionException noKeyAvailable() {
        return new KeyDistributionException(HttpStatus.CONFLICT, "NO_KEY_AVAILABLE",
                "Hết key của gói này. Hãy nhờ Quản lý bán hàng phân bổ thêm.");
    }

    public static KeyDistributionException keyNotFound() {
        return new KeyDistributionException(HttpStatus.NOT_FOUND, "KEY_NOT_FOUND", "Không tìm thấy key này.");
    }

    public static KeyDistributionException batchNotFound() {
        return new KeyDistributionException(HttpStatus.NOT_FOUND, "BATCH_NOT_FOUND", "Không tìm thấy đợt key này.");
    }

    public static KeyDistributionException codeNotStored() {
        return new KeyDistributionException(HttpStatus.CONFLICT, "CODE_NOT_STORED",
                "Key này không có mã lưu trong hệ thống nên không xem lại được.");
    }

    public static KeyDistributionException keyNotIssued() {
        return new KeyDistributionException(HttpStatus.CONFLICT, "KEY_NOT_ISSUED",
                "Key chưa được phát nên chưa xem được mã. Hãy phát key trước.");
    }

    public static KeyDistributionException saleNotEligible() {
        return new KeyDistributionException(HttpStatus.UNPROCESSABLE_ENTITY, "SALE_NOT_ELIGIBLE",
                "Người được chọn không phải nhân viên bán hàng đang hoạt động.");
    }

    /** The stored code could not be read back, e.g. the secret changed since it was written. Whatever was taken is rolled back. */
    public static KeyDistributionException codeUnreadable() {
        return new KeyDistributionException(HttpStatus.SERVICE_UNAVAILABLE, "CODE_UNREADABLE",
                "Không đọc được mã key đã lưu. Hãy báo quản trị viên kiểm tra khóa mã hóa.");
    }

    public static KeyDistributionException cipherUnavailable() {
        return new KeyDistributionException(HttpStatus.SERVICE_UNAVAILABLE, "KEY_CODE_CIPHER_UNAVAILABLE",
                "Hệ thống chưa cấu hình khóa mã hóa key nên chưa thực hiện được thao tác này.");
    }
}
