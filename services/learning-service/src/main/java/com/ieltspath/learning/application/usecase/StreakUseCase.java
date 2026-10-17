package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.service.LearnerTimezones;
import com.ieltspath.learning.domain.repository.StreakRepository;
import com.ieltspath.learning.domain.service.StreakRules;
import com.ieltspath.learning.domain.vo.StreakState;
import com.ieltspath.learning.domain.vo.StreakView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/** The learner's streak: how it stands today, and a freeze bought with points. */
@Service
public class StreakUseCase {
    private final LearnerLock lock;
    private final StreakRepository streaks;
    private final AccessClient access;
    private final LearnerTimezones timezones;
    private final Clock clock = Clock.systemUTC();

    public StreakUseCase(LearnerLock lock, StreakRepository streaks, AccessClient access, LearnerTimezones timezones) {
        this.lock = lock;
        this.streaks = streaks;
        this.access = access;
        this.timezones = timezones;
    }

    /** Applies the empty days up to yesterday and stores the result, so a lost streak is kept as lost. */
    @Transactional
    public StreakView view(UUID userId) {
        lock.lock(userId);
        LocalDate today = today(userId);
        StreakState stored = streaks.find(userId).orElse(StreakState.empty());
        StreakState decided = StreakRules.forRead(stored, today);
        if (!decided.equals(stored)) streaks.save(userId, decided);
        return StreakView.of(decided, today);
    }

    /**
     * Buys a freeze with points at the current price. The freeze covers the next empty day. A repeat of the same
     * {@code requestId} is charged once by Access; the purchase itself then reports the freeze as already owned.
     */
    @Transactional
    public StreakView buyFreeze(UUID userId, UUID requestId) {
        lock.lock(userId);
        LocalDate today = today(userId);
        StreakState state = StreakRules.forRead(streaks.find(userId).orElse(StreakState.empty()), today);
        if (state.freezeAvailable()) {
            throw new LearningRequestException(409, "FREEZE_ALREADY_OWNED", "A freeze is already owned");
        }
        if (state.currentDays() == 0) {
            throw new LearningRequestException(409, "NO_STREAK_TO_PROTECT", "There is no streak to protect");
        }
        if (StreakRules.overLimit(state, today)) {
            throw new LearningRequestException(409, "FREEZE_LIMIT", "Consecutive freezes are at their limit");
        }
        access.debit(userId, StreakRules.freezeCost(state, today), "STREAK_FREEZE", requestId,
                requestId.toString(), "Đóng băng streak");
        StreakState bought = StreakRules.buyFreeze(state, today);
        streaks.save(userId, bought);
        return StreakView.of(bought, today);
    }

    private LocalDate today(UUID userId) {
        return LocalDate.now(clock.withZone(timezones.zoneOf(userId)));
    }
}
