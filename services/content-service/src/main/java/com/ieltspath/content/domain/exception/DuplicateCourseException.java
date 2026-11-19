package com.ieltspath.content.domain.exception;

import java.math.BigDecimal;

public class DuplicateCourseException extends ContentDomainException {
    public DuplicateCourseException(String field, String value) {
        super(messageFor(field, value));
    }

    private static String messageFor(String field, String value) {
        return switch (field) {
            case "code" -> "Mã khóa học \"" + value + "\" đã được dùng. Hãy chọn mã khác.";
            case "bandLevel" -> "Band " + new BigDecimal(value).setScale(1) + " đã có khóa học. Mỗi band chỉ có một khóa học.";
            default -> "Khóa học trùng thông tin \"" + field + "\" đã tồn tại.";
        };
    }
}
