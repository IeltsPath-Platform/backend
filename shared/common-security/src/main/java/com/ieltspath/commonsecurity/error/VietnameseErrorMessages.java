package com.ieltspath.commonsecurity.error;

import java.util.regex.Pattern;

/**
 * Keeps every error message a client can read in Vietnamese with diacritics. A message that already contains Vietnamese
 * letters was written for people and passes through; anything else (an English message from a library or an invariant
 * check) is replaced by a generic sentence for its HTTP status. The error handlers call this where they build a response,
 * and log the original text, so a message nobody translated can never reach the screen in another language.
 */
public final class VietnameseErrorMessages {
    private static final Pattern VIETNAMESE_LETTER = Pattern.compile(
            "[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private VietnameseErrorMessages() {
    }

    /** True when the text has at least one Vietnamese letter, which is how a message written for people is told apart. */
    public static boolean isVietnamese(String message) {
        return message != null && VIETNAMESE_LETTER.matcher(message).find();
    }

    /** The message itself when it is Vietnamese, otherwise the generic sentence for {@code status}. */
    public static String userFacing(int status, String message) {
        return isVietnamese(message) ? message : generic(status);
    }

    public static String generic(int status) {
        return switch (status) {
            case 400 -> "Dữ liệu gửi lên không hợp lệ.";
            case 401 -> "Bạn cần đăng nhập để tiếp tục.";
            case 403 -> "Bạn không có quyền thực hiện thao tác này.";
            case 404 -> "Không tìm thấy dữ liệu được yêu cầu.";
            case 405 -> "Phương thức này không được hỗ trợ.";
            case 409 -> "Thao tác xung đột với trạng thái hiện tại của dữ liệu.";
            case 413 -> "Tệp tải lên vượt quá giới hạn cho phép.";
            case 415 -> "Định dạng dữ liệu không được hỗ trợ.";
            case 422 -> "Dữ liệu chưa đáp ứng điều kiện để thực hiện thao tác này.";
            case 429 -> "Bạn thao tác quá nhanh, vui lòng thử lại sau ít phút.";
            case 503 -> "Dịch vụ tạm thời chưa khả dụng, vui lòng thử lại sau.";
            default -> status >= 500
                    ? "Hệ thống gặp lỗi, vui lòng thử lại sau."
                    : "Có lỗi xảy ra, vui lòng thử lại.";
        };
    }
}
