package com.ieltspath.user.domain.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @Test
    void parseRemovesSpacesDotsDashesAndBrackets() {
        assertThat(PhoneNumber.parse(" 0123 456 789 ").value()).isEqualTo("0123456789");
        assertThat(PhoneNumber.parse("0123.456.789").value()).isEqualTo("0123456789");
        assertThat(PhoneNumber.parse("(0123)-456-789").value()).isEqualTo("0123456789");
    }

    @Test
    void parseKeepsALeadingPlus() {
        assertThat(PhoneNumber.parse("+84 912 345 678").value()).isEqualTo("+84912345678");
    }

    @Test
    void parseTurnsBlankIntoNoPhone() {
        assertThat(PhoneNumber.parse(null).value()).isNull();
        assertThat(PhoneNumber.parse("   ").value()).isNull();
    }

    @Test
    void parseAcceptsEightAndFifteenDigits() {
        assertThat(PhoneNumber.parse("12345678").value()).isEqualTo("12345678");
        assertThat(PhoneNumber.parse("123456789012345").value()).isEqualTo("123456789012345");
    }

    @Test
    void parseRejectsLettersAndWrongLength() {
        for (String bad : new String[]{"abc", "0123abc789", "1234567", "1234567890123456", "++84912345678", "84+912345678"}) {
            assertThatThrownBy(() -> PhoneNumber.parse(bad))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Số điện thoại không hợp lệ");
        }
    }

    @Test
    void constructorStaysLenientSoOldRowsStillLoad() {
        assertThat(new PhoneNumber(" 222 ").value()).isEqualTo("222");
        assertThat(new PhoneNumber(" ").value()).isNull();
    }
}
