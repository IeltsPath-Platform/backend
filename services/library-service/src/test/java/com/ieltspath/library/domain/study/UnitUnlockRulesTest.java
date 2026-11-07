package com.ieltspath.library.domain.study;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UnitUnlockRulesTest {
    @Test void prerequisitesChangesPreserveCompletedAndActiveUnits() {
        UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID(),inserted=UUID.randomUUID();
        assertEquals(List.of("AVAILABLE","LOCKED","LOCKED"),UnitUnlockRules.statuses(List.of(a,b,c),Set.of(),Set.of()));
        assertEquals(List.of("COMPLETED","AVAILABLE","LOCKED"),UnitUnlockRules.statuses(List.of(a,b,c),Set.of(a),Set.of()));
        assertEquals(List.of("COMPLETED","AVAILABLE","IN_PROGRESS","LOCKED"),UnitUnlockRules.statuses(List.of(a,inserted,b,c),Set.of(a),Set.of(b)));
        assertEquals(List.of("AVAILABLE","COMPLETED"),UnitUnlockRules.statuses(List.of(inserted,b),Set.of(b),Set.of()));
    }
}
