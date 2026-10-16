package com.ieltspath.assessment.domain.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AttemptTypeTest {
    @Test
    void practiceTestUsesOfficialPractice() {
        assertEquals(AttemptType.OFFICIAL_PRACTICE, AttemptType.forContentPackageType("PRACTICE_TEST"));
    }
}
