package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

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

    @Test
    void aCourseIsPaidUnlessFlaggedFreeAndAnUpdateThatDoesNotSendTheFlagKeepsIt() {
        assertThat(Course.create("PAID", "Paid", new BigDecimal("6.0")).isFree()).isFalse();

        Course free = Course.create("FREE", "Free", new BigDecimal("4.0"), null, null, Map.of(), null, true);
        assertThat(free.isFree()).isTrue();
        free.update("Free course", new BigDecimal("4.0"), null);
        assertThat(free.isFree()).isTrue();
        free.update("Free course", new BigDecimal("4.0"), null, null, null, null, null, false);
        assertThat(free.isFree()).isFalse();
    }

    @Test
    void aFreeCourseCannotHaveUnlockPricesAndARefusedUpdateChangesNothing() {
        Map<Skill, Integer> prices = Map.of(Skill.READING, 40);
        assertThatThrownBy(() -> Course.create("FREE", "Free", new BigDecimal("4.0"), null, null, prices, null, true))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("free course");

        Course free = Course.create("FREE", "Free", new BigDecimal("4.0"), null, null, Map.of(), null, true);
        assertThatThrownBy(() -> free.update("Renamed", new BigDecimal("4.0"), null, null, null, prices, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(free.getName()).isEqualTo("Free");
        assertThat(free.getSkillUnlockPoints()).isEmpty();

        Course priced = Course.create("PRICED", "Priced", new BigDecimal("6.0"), null, null, prices, null, false);
        assertThatThrownBy(() -> priced.update("Priced", new BigDecimal("6.0"), null, null, null, null, null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(priced.isFree()).isFalse();
    }

    @Test
    void aBannerImageFollowsTheSameKeyRulesAsTheCoverAndOnlyAnUpdateThatGivesItChangesIt() {
        Course course = Course.create("BANNER", "Banner", new BigDecimal("6.0"), "content-image/u/cover.png", null,
                Map.of(), null, false, "content-image/u/banner.png");
        assertThat(course.getBannerImageUrl()).isEqualTo("content-image/u/banner.png");

        // An update that does not mention the banner keeps it; one that does replaces it, and null removes it.
        course.update("Banner", new BigDecimal("6.0"), null, "content-image/u/cover.png", null, null, null, null);
        assertThat(course.getBannerImageUrl()).isEqualTo("content-image/u/banner.png");
        course.update("Banner", new BigDecimal("6.0"), null, "content-image/u/cover.png", null, null, null, null,
                "https://images.example/wide.jpg");
        assertThat(course.getBannerImageUrl()).isEqualTo("https://images.example/wide.jpg");
        course.update("Banner", new BigDecimal("6.0"), null, "content-image/u/cover.png", null, null, null, null, null);
        assertThat(course.getBannerImageUrl()).isNull();
        assertThat(course.getCoverImageUrl()).isEqualTo("content-image/u/cover.png");

        for (String invalid : new String[]{"content-image/../x", "javascript:alert(1)", "media/x"}) {
            assertThatThrownBy(() -> Course.create("BAD", "Bad", new BigDecimal("6.0"), null, null, Map.of(), null,
                    false, invalid)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void acceptsOnlySafeCourseImageKeysAndKeepsLegacyHttpsUrls() {
        Course keyed = Course.create("KEYED", "Keyed", new BigDecimal("5.5"),
                " content-image/user-1/cover.png ", null);
        assertThat(keyed.getCoverImageUrl()).isEqualTo("content-image/user-1/cover.png");

        Course legacy = Course.create("LEGACY", "Legacy", new BigDecimal("6.0"),
                "https://images.example/cover.jpg", null);
        assertThat(legacy.getCoverImageUrl()).isEqualTo("https://images.example/cover.jpg");

        for (String invalid : new String[]{"content-image/../x", "/content-image/x", "media/x",
                "javascript:alert(1)", "content-image/" + "x".repeat(500)}) {
            assertThatThrownBy(() -> Course.create("INVALID", "Invalid", new BigDecimal("5.5"), invalid, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(Course.create("BLANK", "Blank", new BigDecimal("5.5"), " ", null).getCoverImageUrl()).isNull();
    }

    @Test
    void codeAndNameAreLimitedTo100And255Characters() {
        Course longest = Course.create("C".repeat(100), "N".repeat(255), new BigDecimal("5.0"));
        assertThat(longest.getCode()).hasSize(100);
        assertThat(longest.getName()).hasSize(255);
        assertThatThrownBy(() -> Course.create("C".repeat(101), "Name", new BigDecimal("5.0")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("code exceeds 100 characters");
        assertThatThrownBy(() -> Course.create("CODE", "N".repeat(256), new BigDecimal("5.0")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("name exceeds 255 characters");
    }

    @Test
    void theLowestAndHighestBandLevelsAreAccepted() {
        assertThat(Course.create("IELTS_0", "Zero", new BigDecimal("0.0")).getBandLevel()).isEqualByComparingTo("0.0");
        assertThat(Course.create("IELTS_9", "Nine", new BigDecimal("9.0")).getBandLevel()).isEqualByComparingTo("9.0");
    }

    @Test
    void aComboPriceLiesBetweenTheHighestSkillPriceAndTheirSum() {
        Map<Skill, Integer> prices = Map.of(Skill.READING, 40, Skill.LISTENING, 60);
        assertThatThrownBy(() -> Course.create("P1", "Priced", new BigDecimal("6.0"), null, null, prices, 50, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("combo unlock price must be at least the highest skill price");
        assertThatThrownBy(() -> Course.create("P2", "Priced", new BigDecimal("6.0"), null, null, prices, 120, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("combo unlock price must not exceed the sum of skill prices");
        assertThatThrownBy(() -> Course.create("P3", "Priced", new BigDecimal("6.0"), null, null,
                Map.of(Skill.READING, 0), null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("skill unlock prices must be positive integers");
        assertThat(Course.create("P4", "Priced", new BigDecimal("6.0"), null, null, prices, 60, false)
                .getComboUnlockPoints()).isEqualTo(60);
        assertThat(Course.create("P5", "Priced", new BigDecimal("6.0"), null, null, prices, 100, false)
                .getComboUnlockPoints()).isEqualTo(100);
    }
}
