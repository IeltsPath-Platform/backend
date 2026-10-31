package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.exception.UnlockPricingException;
import com.ieltspath.learning.domain.vo.CourseUnlockTarget;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnlockPricingTest {
    private final UnlockPricing pricing = new UnlockPricing();
    private final Set<LearningSkill> available = Set.of(LearningSkill.READING, LearningSkill.LISTENING,
            LearningSkill.WRITING);
    private final Map<LearningSkill, Integer> prices = Map.of(LearningSkill.READING, 100,
            LearningSkill.LISTENING, 140, LearningSkill.WRITING, 180);

    @Test
    void skillUsesItsCourseSpecificPrice() {
        assertThat(pricing.price(CourseUnlockTarget.READING, prices, 350, available, Map.of())).isEqualTo(100);
        assertThat(pricing.price(CourseUnlockTarget.WRITING, prices, 350, available, Map.of())).isEqualTo(180);
    }

    @Test
    void comboDeductsWhatWasActuallyPaidAndFloorsAtZero() {
        assertThat(pricing.price(CourseUnlockTarget.COMBO, prices, 350, available, Map.of())).isEqualTo(350);
        assertThat(pricing.price(CourseUnlockTarget.COMBO, prices, 350, available,
                Map.of(LearningSkill.READING, 100))).isEqualTo(250);
        assertThat(pricing.price(CourseUnlockTarget.COMBO, prices, 350, available,
                Map.of(LearningSkill.READING, 100, LearningSkill.LISTENING, 140,
                        LearningSkill.WRITING, 180))).isEqualTo(0);
    }

    @Test
    void pendingAndUnavailableSkillsAreNotPricedAsOwnedOrForSale() {
        assertThat(pricing.price(CourseUnlockTarget.COMBO, prices, 350, available, Map.of())).isEqualTo(350);
        assertThatThrownBy(() -> pricing.price(CourseUnlockTarget.COMBO, prices, null, available, Map.of()))
                .isInstanceOf(UnlockPricingException.class)
                .extracting("code").isEqualTo(UnlockPricingException.Code.NOT_FOR_SALE);
        assertThatThrownBy(() -> pricing.price(CourseUnlockTarget.SPEAKING, prices, 350, available, Map.of()))
                .isInstanceOf(UnlockPricingException.class)
                .extracting("code").isEqualTo(UnlockPricingException.Code.SKILL_NOT_AVAILABLE);
    }
}
