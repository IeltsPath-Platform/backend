package com.ieltspath.content.domain.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TrueFalseNotGivenTrapsTest {
    @Test
    void mapsNotGivenToFalseWhenFalseIsAnOption() {
        assertThat(TrueFalseNotGivenTraps.distractorsFor("NOT_GIVEN", Set.of("TRUE", "FALSE", "NOT_GIVEN")))
                .isEqualTo(java.util.Map.of("FALSE", "TFNG_NG_AS_FALSE"));
    }

    @Test
    void mapsNotGivenToNoWhenNoIsAnOption() {
        assertThat(TrueFalseNotGivenTraps.distractorsFor("NOT_GIVEN", Set.of("YES", "NO", "NOT_GIVEN")))
                .isEqualTo(java.util.Map.of("NO", "TFNG_NG_AS_FALSE"));
    }

    @Test
    void mapsFalseOrNoToNotGivenWhenItIsAnOption() {
        assertThat(TrueFalseNotGivenTraps.distractorsFor("FALSE", Set.of("TRUE", "FALSE", "NOT_GIVEN")))
                .isEqualTo(java.util.Map.of("NOT_GIVEN", "TFNG_FALSE_AS_NG"));
        assertThat(TrueFalseNotGivenTraps.distractorsFor("NO", Set.of("YES", "NO", "NOT_GIVEN")))
                .isEqualTo(java.util.Map.of("NOT_GIVEN", "TFNG_FALSE_AS_NG"));
    }

    @Test
    void doesNotMapTrueOrYesAnswers() {
        assertThat(TrueFalseNotGivenTraps.distractorsFor("TRUE", Set.of("TRUE", "FALSE", "NOT_GIVEN"))).isEmpty();
        assertThat(TrueFalseNotGivenTraps.distractorsFor("YES", Set.of("YES", "NO", "NOT_GIVEN"))).isEmpty();
    }

    @Test
    void doesNotMapMissingOrUnsupportedOptions() {
        assertThat(TrueFalseNotGivenTraps.distractorsFor("NOT_GIVEN", Set.of("TRUE", "NOT_GIVEN"))).isEmpty();
        assertThat(TrueFalseNotGivenTraps.distractorsFor("FALSE", Set.of("TRUE", "FALSE"))).isEmpty();
        assertThat(TrueFalseNotGivenTraps.distractorsFor(null, Set.of("TRUE", "FALSE", "NOT_GIVEN"))).isEmpty();
        assertThat(TrueFalseNotGivenTraps.distractorsFor("NOT_G GIVEN", Set.of("TRUE", "FALSE", "NOT_GIVEN"))).isEmpty();
    }
}
