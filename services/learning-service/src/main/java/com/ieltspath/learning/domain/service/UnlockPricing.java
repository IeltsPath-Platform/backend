package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.CourseUnlockTarget;
import com.ieltspath.learning.domain.exception.UnlockPricingException;
import com.ieltspath.learning.domain.vo.LearningSkill;

import java.util.Map;
import java.util.Set;

public final class UnlockPricing {
    public int price(CourseUnlockTarget target, Map<LearningSkill, Integer> skillPrices, Integer comboPrice,
                     Set<LearningSkill> availableSkills, Map<LearningSkill, Integer> activePaidSkillPrices) {
        if (target == CourseUnlockTarget.COMBO) {
            if (comboPrice == null) throw new UnlockPricingException(UnlockPricingException.Code.NOT_FOR_SALE);
            int alreadyPaid = activePaidSkillPrices.values().stream().mapToInt(Integer::intValue).sum();
            return Math.max(0, comboPrice - alreadyPaid);
        }
        LearningSkill skill = target.skill();
        if (!availableSkills.contains(skill)) {
            throw new UnlockPricingException(UnlockPricingException.Code.SKILL_NOT_AVAILABLE);
        }
        Integer price = skillPrices.get(skill);
        if (price == null) throw new UnlockPricingException(UnlockPricingException.Code.NOT_FOR_SALE);
        return price;
    }
}
