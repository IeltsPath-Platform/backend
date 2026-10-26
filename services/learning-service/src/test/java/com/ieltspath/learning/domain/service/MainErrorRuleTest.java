package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.application.service.ErrorTrapCatalog.ErrorTraps;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.vo.MasteryTier;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MainErrorRuleTest {
    private static final UUID USER = UUID.randomUUID();
    private static final UUID PARENT = UUID.randomUUID();
    private static final UUID HIGHER = UUID.randomUUID();
    private static final UUID LOWER = UUID.randomUUID();

    @Test
    void hasNoMainErrorWhenParentHasNoTrapChildren() {
        assertThat(MainErrorRule.mainErrorCode(PARENT, List.of(), traps(Map.of(), Map.of())))
                .isEmpty();
    }

    @Test
    void ignoresInsufficientTrapProfiles() {
        assertThat(MainErrorRule.mainErrorCode(PARENT, List.of(profile(LOWER, .1, MasteryTier.INSUFFICIENT)),
                traps(Map.of("A_ERROR", LOWER), Map.of(LOWER, PARENT)))).isEmpty();
    }

    @Test
    void choosesLowestMasteryThenAlphabeticalErrorCode() {
        var traps = traps(Map.of("Z_ERROR", LOWER, "A_ERROR", HIGHER), Map.of(LOWER, PARENT, HIGHER, PARENT));
        var profiles = List.of(profile(HIGHER, .4, MasteryTier.PRIORITY), profile(LOWER, .2, MasteryTier.PRIORITY));
        assertThat(MainErrorRule.mainErrorCode(PARENT, profiles, traps)).contains("Z_ERROR");

        profiles = List.of(profile(LOWER, .2, MasteryTier.PRIORITY), profile(HIGHER, .2, MasteryTier.MAINTAIN));
        assertThat(MainErrorRule.mainErrorCode(PARENT, profiles, traps)).contains("A_ERROR");
    }

    private static MasteryProfile profile(UUID kp, double mastery, MasteryTier tier) {
        return new MasteryProfile(USER, kp, mastery, tier, 4, 0, Instant.EPOCH, null, null, List.of());
    }

    private static ErrorTraps traps(Map<String, UUID> codeToTrap, Map<UUID, UUID> parentByTrap) {
        return new ErrorTraps(codeToTrap, parentByTrap, Map.of(PARENT, List.copyOf(parentByTrap.keySet())));
    }
}
