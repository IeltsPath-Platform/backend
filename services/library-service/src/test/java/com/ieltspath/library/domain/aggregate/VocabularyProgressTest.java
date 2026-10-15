package com.ieltspath.library.domain.aggregate;

import com.ieltspath.library.domain.exception.ConflictException;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class VocabularyProgressTest {
    private final Instant now=Instant.parse("2026-10-15T00:00:00Z");
    private VocabularyProgress fresh() { return VocabularyProgress.start(UUID.randomUUID(),UUID.randomUUID(),now); }
    @Test void rememberingAtEachDueDateUsesOneThreeSevenFourteenDaySchedule() {
        var p=fresh();
        int[] days={1,3,7,14,14};
        VocabularyProgress.State[] states={VocabularyProgress.State.L1,VocabularyProgress.State.L2,
                VocabularyProgress.State.L3,VocabularyProgress.State.RETAINED,VocabularyProgress.State.RETAINED};
        for(int n=0;n<days.length;n++) {
            Instant reviewed=p.nextReviewAt();
            p=p.answer(p.stateVersion(),VocabularyProgress.Outcome.REMEMBERED,reviewed);
            assertEquals(states[n],p.state());assertEquals(reviewed.plus(Duration.ofDays(days[n])),p.nextReviewAt());
        }
    }
    @Test void forgettingSchedulesTenMinutesAndReturnsToLevelOne() {
        var p=new VocabularyProgress(UUID.randomUUID(),UUID.randomUUID(),VocabularyProgress.State.RETAINED,6,true,now,now,now);
        var answer=p.answer(6,VocabularyProgress.Outcome.FORGOT,now);
        assertEquals(VocabularyProgress.State.L1,answer.state());assertEquals(now.plusSeconds(600),answer.nextReviewAt());
    }
    @Test void earlyPracticeAndConcurrentVersionCannotAdvance() {
        var p=fresh().answer(0,VocabularyProgress.Outcome.REMEMBERED,now);
        assertThrows(ConflictException.class,()->p.answer(1,VocabularyProgress.Outcome.REMEMBERED,now));
        assertThrows(ConflictException.class,()->p.answer(0,VocabularyProgress.Outcome.REMEMBERED,p.nextReviewAt()));
    }
    @Test void archiveInvalidatesAssignedVersionsAndReactivationPreservesHistory() {
        var p=fresh().answer(0,VocabularyProgress.Outcome.REMEMBERED,now).activate(false);
        assertThrows(ConflictException.class,()->p.answer(p.stateVersion(),VocabularyProgress.Outcome.REMEMBERED,p.nextReviewAt()));
        var active=p.activate(true);
        assertEquals(p.state(),active.state());assertEquals(p.nextReviewAt(),active.nextReviewAt());
        assertEquals(p.stateVersion()+1,active.stateVersion());
        assertSame(active,active.activate(true));
    }
}
