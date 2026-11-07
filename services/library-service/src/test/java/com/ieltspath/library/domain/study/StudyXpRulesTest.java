package com.ieltspath.library.domain.study;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudyXpRulesTest {
    @Test void levelsAndRewardBoundaries() {
        assertEquals(1,StudyXpRules.level(0,500));assertEquals(1,StudyXpRules.level(499,500));assertEquals(2,StudyXpRules.level(500,500));assertEquals(2,StudyXpRules.level(999,500));assertEquals(3,StudyXpRules.level(1000,500));
        assertEquals(1000,StudyXpRules.nextLevelAt(500,500));
        assertEquals(30,StudyXpRules.award(true,true,20,10));assertEquals(20,StudyXpRules.award(true,false,20,10));assertEquals(0,StudyXpRules.award(false,true,20,10));
    }
}
