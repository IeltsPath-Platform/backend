package com.ieltspath.commonsecurity.error;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class VietnameseErrorMessagesTest {
    @Test
    void aMessageWithVietnameseLettersPassesThrough() {
        assertThat(VietnameseErrorMessages.userFacing(409, "Mã khóa học đã được dùng")).isEqualTo("Mã khóa học đã được dùng");
        assertThat(VietnameseErrorMessages.userFacing(400, "ĐỀ BÀI không hợp lệ")).isEqualTo("ĐỀ BÀI không hợp lệ");
    }

    @Test
    void anyOtherMessageBecomesTheGenericSentenceOfItsStatus() {
        assertThat(VietnameseErrorMessages.userFacing(400, "Validation failed")).isEqualTo("Dữ liệu gửi lên không hợp lệ.");
        assertThat(VietnameseErrorMessages.userFacing(404, "Course not found")).isEqualTo("Không tìm thấy dữ liệu được yêu cầu.");
        assertThat(VietnameseErrorMessages.userFacing(409, null)).isEqualTo("Thao tác xung đột với trạng thái hiện tại của dữ liệu.");
        assertThat(VietnameseErrorMessages.userFacing(500, "An unexpected internal error occurred"))
                .isEqualTo("Hệ thống gặp lỗi, vui lòng thử lại sau.");
        assertThat(VietnameseErrorMessages.userFacing(502, "bad gateway")).isEqualTo("Hệ thống gặp lỗi, vui lòng thử lại sau.");
    }

    @Test
    void everyGenericSentenceIsVietnamese() {
        Stream.of(400, 401, 403, 404, 405, 409, 413, 415, 422, 429, 500, 503, 418)
                .forEach(status -> assertThat(VietnameseErrorMessages.isVietnamese(VietnameseErrorMessages.generic(status)))
                        .as("status %s", status).isTrue());
    }

    private record Form(@NotBlank String name, @Size(max = 3) String code) { }

    @Test
    void constraintsWithoutTheirOwnMessageAreInVietnameseWhateverTheLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                Validator validator = factory.getValidator();
                var violations = validator.validate(new Form(" ", "abcdef"));
                Set<String> messages = new java.util.TreeSet<>();
                violations.forEach(v -> messages.add(v.getMessage()));
                assertThat(messages).allSatisfy(message -> assertThat(VietnameseErrorMessages.isVietnamese(message)).isTrue());
                assertThat(messages).contains("Không được để trống");
            }
        } finally {
            Locale.setDefault(previous);
        }
    }
}
