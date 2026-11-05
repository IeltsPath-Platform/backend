package com.ieltspath.user.domain.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FullNameTest {

    @Test
    void trimsTheName() {
        assertThat(new FullName("  Nguyễn Văn A  ").value()).isEqualTo("Nguyễn Văn A");
    }

    @Test
    void acceptsExactlyTheMaximumLength() {
        assertThat(new FullName("n".repeat(FullName.MAX_LENGTH)).value()).hasSize(FullName.MAX_LENGTH);
    }

    @Test
    void countsTheLengthAfterTrimming() {
        String padded = "  " + "n".repeat(FullName.MAX_LENGTH) + "  ";
        assertThat(new FullName(padded).value()).hasSize(FullName.MAX_LENGTH);
    }

    @Test
    void rejectsANameLongerThanTheColumn() {
        assertThatThrownBy(() -> new FullName("n".repeat(FullName.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Họ tên không được vượt quá 150 ký tự");
    }

    @Test
    void rejectsNullAndBlank() {
        assertThatThrownBy(() -> new FullName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Họ tên không được để trống");
        assertThatThrownBy(() -> new FullName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Họ tên không được để trống");
    }
}
