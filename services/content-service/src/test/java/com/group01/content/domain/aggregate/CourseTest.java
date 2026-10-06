package com.group01.content.domain.aggregate;

import com.group01.content.domain.vo.ContentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class CourseTest {
    @Test
    void rejectsBandsOutsideTheIeltsHalfBandScaleAndBlankNames() {
        for (String band : new String[]{"5.25", "9.5", "-0.5"}) {
            assertThatThrownBy(() -> Course.create("IELTS", "IELTS", new BigDecimal(band)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> Course.create("IELTS", " ", new BigDecimal("5.5")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Course.create("IELTS", "IELTS", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesBandsAndUpdatesEditableFieldsWithoutChangingCode() {
        Course course = Course.create(" IELTS_5_5 ", " IELTS 5.5 ", new BigDecimal("5.50"));
        assertThat(course.getBandLevel()).isEqualTo(new BigDecimal("5.5"));
        assertThat(course.getStatus()).isEqualTo(ContentStatus.ACTIVE);
        course.update(" IELTS 6.5 ", new BigDecimal("6.5"), ContentStatus.INACTIVE);
        assertThat(course.getCode()).isEqualTo("IELTS_5_5");
        assertThat(course.getName()).isEqualTo("IELTS 6.5");
        assertThat(course.getBandLevel()).isEqualTo(new BigDecimal("6.5"));
        assertThat(course.getStatus()).isEqualTo(ContentStatus.INACTIVE);
        assertThatThrownBy(() -> course.update("", new BigDecimal("7.0"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(course.getName()).isEqualTo("IELTS 6.5");
    }
}
