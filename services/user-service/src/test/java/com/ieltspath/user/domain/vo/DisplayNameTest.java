package com.ieltspath.user.domain.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisplayNameTest {

    @Test
    void neverAllowsLessThanAFullName() {
        // A new profile is named after the account, so a valid full name must always be a valid display name.
        assertThat(DisplayName.MAX_LENGTH).isGreaterThanOrEqualTo(FullName.MAX_LENGTH);
        assertThat(new DisplayName(new FullName("n".repeat(FullName.MAX_LENGTH)).value()).value())
                .hasSize(FullName.MAX_LENGTH);
    }

    @Test
    void trimsAndAcceptsTheMaximumLength() {
        assertThat(new DisplayName("  Lan  ").value()).isEqualTo("Lan");
        assertThat(new DisplayName("n".repeat(DisplayName.MAX_LENGTH)).value()).hasSize(DisplayName.MAX_LENGTH);
    }

    @Test
    void rejectsANameLongerThanTheColumn() {
        assertThatThrownBy(() -> new DisplayName("n".repeat(DisplayName.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tên hiển thị không được vượt quá 150 ký tự");
    }

    @Test
    void rejectsNullAndBlank() {
        assertThatThrownBy(() -> new DisplayName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tên hiển thị không được để trống");
        assertThatThrownBy(() -> new DisplayName(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tên hiển thị không được để trống");
    }
}
