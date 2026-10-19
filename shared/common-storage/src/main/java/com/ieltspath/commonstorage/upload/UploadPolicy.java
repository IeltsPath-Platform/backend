package com.ieltspath.commonstorage.upload;

import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.commonstorage.UploadRejectedException.Reason;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Chính sách nhận file: loại được phép và dung lượng tối đa.
 *
 * <p>Module không chứa chính sách dựng sẵn cho avatar hay Speaking. Đó là nghiệp vụ của từng service.
 */
public record UploadPolicy(Set<String> allowedContentTypes, long maxBytes) {

    /** Trần để {@code maxBytes + 1} vẫn vừa một mảng {@code byte}. */
    private static final long ABSOLUTE_MAX = Integer.MAX_VALUE - 8;

    public UploadPolicy {
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            throw new IllegalArgumentException("upload policy needs at least one allowed content type");
        }
        if (maxBytes <= 0 || maxBytes > ABSOLUTE_MAX) {
            throw new IllegalArgumentException("upload policy maxBytes is out of range");
        }
        allowedContentTypes = Set.copyOf(allowedContentTypes);
    }

    /**
     * Tạo chính sách. MIME được chuẩn hóa qua {@link ContentTypes}. Wildcard ({@code audio/*}, {@code *}/*) bị từ
     * chối: phải nêu rõ từng loại.
     */
    public static UploadPolicy of(long maxBytes, String... allowedContentTypes) {
        Set<String> normalized = new HashSet<>();
        for (String declared : allowedContentTypes) {
            String type = ContentTypes.normalize(declared);
            if (type == null || type.contains("*")) {
                throw new IllegalArgumentException("upload policy needs explicit content types");
            }
            normalized.add(type);
        }
        return new UploadPolicy(normalized, maxBytes);
    }

    /** Kiểm sớm theo kích thước khai báo, trước khi đọc body. */
    public void requireSize(long sizeBytes) {
        if (sizeBytes > maxBytes) {
            throw new UploadRejectedException(Reason.TOO_LARGE);
        }
    }

    /**
     * Đọc tối đa {@code maxBytes + 1} byte. Vượt giới hạn thì ném {@code TOO_LARGE}. Không tin {@code Content-Length},
     * nên bộ nhớ của bước này bị chặn ngay cả khi header nói dối hoặc body chunked.
     *
     * <p>Không ngăn được Spring multipart đã đệm body trước đó: service phải đặt giới hạn multipart riêng.
     */
    public byte[] readBounded(InputStream in) throws IOException {
        byte[] content = in.readNBytes((int) maxBytes + 1);
        if (content.length > maxBytes) {
            throw new UploadRejectedException(Reason.TOO_LARGE);
        }
        return content;
    }

    /**
     * Kiểm nội dung theo thứ tự: rỗng, quá lớn, loại khai báo không được phép, rồi magic bytes không khớp loại khai
     * báo. Trả MIME chuẩn để lưu.
     */
    public String check(byte[] content, String declaredContentType) {
        if (content == null || content.length == 0) {
            throw new UploadRejectedException(Reason.EMPTY);
        }
        if (content.length > maxBytes) {
            throw new UploadRejectedException(Reason.TOO_LARGE);
        }
        String declared = ContentTypes.normalize(declaredContentType);
        if (declared == null || !allowedContentTypes.contains(declared)) {
            throw new UploadRejectedException(Reason.TYPE_NOT_ALLOWED);
        }
        Optional<String> sniffed = ContentSniffer.detect(content);
        if (sniffed.isEmpty() || !sniffed.get().equals(declared)) {
            throw new UploadRejectedException(Reason.CONTENT_MISMATCH);
        }
        return declared;
    }
}
