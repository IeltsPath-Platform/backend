package com.ieltspath.user.domain.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    @Test
    void countsCharactersForTheMinimum() {
        assertThat(PasswordPolicy.isWithinLimits("a".repeat(5))).isFalse();
        assertThat(PasswordPolicy.isWithinLimits("a".repeat(6))).isTrue();
    }

    @Test
    void countsBytesForTheMaximum() {
        assertThat(PasswordPolicy.isWithinLimits("a".repeat(72))).isTrue();
        assertThat(PasswordPolicy.isWithinLimits("a".repeat(73))).isFalse();
    }

    @Test
    void aVietnameseLetterWithTwoMarksTakesThreeBytes() {
        assertThat(PasswordPolicy.isWithinLimits("ậ".repeat(24))).isTrue();
        assertThat(PasswordPolicy.isWithinLimits("ậ".repeat(25))).isFalse();
        assertThat(PasswordPolicy.exceedsMaxBytes("ậ".repeat(25))).isTrue();
    }

    @Test
    void nullIsNotWithinLimits() {
        assertThat(PasswordPolicy.isWithinLimits(null)).isFalse();
        assertThat(PasswordPolicy.exceedsMaxBytes(null)).isFalse();
    }
}
