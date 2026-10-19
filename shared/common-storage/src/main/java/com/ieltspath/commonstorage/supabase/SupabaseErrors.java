package com.ieltspath.commonstorage.supabase;

import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.StorageException.Reason;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Đổi mọi lỗi của Supabase thành {@link StorageException}.
 *
 * <p>Chỉ đọc {@code statusCode} trong body, không chép {@code message}. Smoke cho thấy HTTP 400 có thể kèm
 * {@code statusCode} 409 hoặc 404 trong body, nên mã trong body được ưu tiên.
 */
final class SupabaseErrors {

    private static final Pattern BODY_STATUS = Pattern.compile("\"statusCode\"\\s*:\\s*\"?(\\d{3})\"?");

    private SupabaseErrors() {
    }

    /**
     * @param writeOp true với {@code put} và {@code delete}: lỗi giữa chừng có thể đã ghi hoặc xóa object
     */
    static StorageException map(String operation, String bucket, String key, RuntimeException failure,
                                boolean writeOp) {
        if (failure instanceof StorageException existing) {
            return existing;
        }
        String ref = StorageException.ref(bucket, key);
        if (failure instanceof RestClientResponseException response) {
            int status = effectiveStatus(response);
            return new StorageException(operation, reasonFor(status), status, false, ref);
        }
        if (failure instanceof ResourceAccessException) {
            return new StorageException(operation, Reason.UNAVAILABLE, 0, writeOp, ref);
        }
        if (failure instanceof RestClientException) {
            return new StorageException(operation, Reason.UNEXPECTED_RESPONSE, 0, writeOp, ref);
        }
        return new StorageException(operation, Reason.UNAVAILABLE, 0, writeOp, ref);
    }

    /** Mã trong body nếu có, không thì HTTP status. Không ghi body ra đâu cả. */
    static int effectiveStatus(RestClientResponseException response) {
        Matcher matcher = BODY_STATUS.matcher(response.getResponseBodyAsString());
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return response.getStatusCode().value();
    }

    static Reason reasonFor(int status) {
        return switch (status) {
            case 400 -> Reason.BAD_REQUEST;
            case 401 -> Reason.UNAUTHORIZED;
            case 403 -> Reason.FORBIDDEN;
            case 404 -> Reason.NOT_FOUND;
            case 409 -> Reason.ALREADY_EXISTS;
            case 413 -> Reason.PAYLOAD_TOO_LARGE;
            case 429 -> Reason.RATE_LIMITED;
            default -> Reason.UPSTREAM_ERROR;
        };
    }
}
