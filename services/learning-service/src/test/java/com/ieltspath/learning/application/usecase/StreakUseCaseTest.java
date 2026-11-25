package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.service.LearnerTimezones;
import com.ieltspath.learning.domain.repository.StreakRepository;
import com.ieltspath.learning.domain.service.StreakRules;
import com.ieltspath.learning.domain.vo.StreakState;
import com.ieltspath.learning.domain.vo.StreakView;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A streak freeze is bought with points; the streak a learner reads is stored as decided. */
class StreakUseCaseTest {
    private final UUID user = UUID.randomUUID();
    private final LearnerLock lock = mock(LearnerLock.class);
    private final AccessClient access = mock(AccessClient.class);
    private final LearnerTimezones timezones = mock(LearnerTimezones.class);
    private final MemoryStreaks streaks = new MemoryStreaks();
    private final StreakUseCase useCase = new StreakUseCase(lock, streaks, access, timezones);
    // The use case reads the real clock in the learner's zone; UTC keeps the test's "today" the same day.
    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    StreakUseCaseTest() {
        when(timezones.zoneOf(user)).thenReturn(ZoneOffset.UTC);
    }

    /** A streak of {@code days} days whose last answer is on {@code lastDay}. */
    private static StreakState streak(int days, LocalDate lastDay) {
        StreakState state = StreakState.empty();
        for (int i = days - 1; i >= 0; i--) state = StreakRules.recordAnswer(state, lastDay.minusDays(i)).state();
        return state;
    }

    private static int status(Throwable error) { return ((LearningRequestException) error).getStatus(); }
    private static String code(Throwable error) { return ((LearningRequestException) error).getCode(); }

    @Test
    void aFreezeIsChargedAtTheCurrentPriceUnderTheLearnerLockAndStored() {
        streaks.put(user, streak(3, today));
        UUID request = UUID.randomUUID();

        StreakView view = useCase.buyFreeze(user, request);

        var order = inOrder(lock, access);
        order.verify(lock).lock(user);
        order.verify(access).debit(user, 10, "STREAK_FREEZE", request, request.toString(), "Đóng băng streak");
        assertThat(view.freezeAvailable()).isTrue();
        assertThat(streaks.get(user).freezeAvailable()).isTrue();
    }

    @Test
    void aSecondFreezeIsRefusedWhileOneIsOwned() {
        streaks.put(user, StreakRules.buyFreeze(streak(3, today), today));
        assertThatThrownBy(() -> useCase.buyFreeze(user, UUID.randomUUID()))
                .satisfies(error -> {
                    assertThat(status(error)).isEqualTo(409);
                    assertThat(code(error)).isEqualTo("FREEZE_ALREADY_OWNED");
                });
        verify(access, never()).debit(any(), anyInt(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void aLearnerWithoutAStreakHasNothingToProtect() {
        assertThatThrownBy(() -> useCase.buyFreeze(user, UUID.randomUUID()))
                .satisfies(error -> {
                    assertThat(status(error)).isEqualTo(409);
                    assertThat(code(error)).isEqualTo("NO_STREAK_TO_PROTECT");
                });
        verify(access, never()).debit(any(), anyInt(), anyString(), any(), anyString(), anyString());
        assertThat(streaks.saves).isZero();
    }

    @Test
    void aSixthConsecutiveFreezeIsRefused() {
        // Five frozen days in a row ending yesterday: the next freeze would be the sixth.
        streaks.put(user, new StreakState(3, 3, today.minusDays(6), today.minusDays(8), today.minusDays(1), false,
                null, today.minusDays(1), 5, 0));
        assertThatThrownBy(() -> useCase.buyFreeze(user, UUID.randomUUID()))
                .satisfies(error -> {
                    assertThat(status(error)).isEqualTo(409);
                    assertThat(code(error)).isEqualTo("FREEZE_LIMIT");
                });
        verify(access, never()).debit(any(), anyInt(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void theFreezePriceRisesWithEachConsecutiveFrozenDay() {
        streaks.put(user, new StreakState(3, 3, today.minusDays(2), today.minusDays(4), today.minusDays(1), false,
                null, today.minusDays(1), 1, 0));
        UUID request = UUID.randomUUID();
        useCase.buyFreeze(user, request);
        verify(access).debit(user, 20, "STREAK_FREEZE", request, request.toString(), "Đóng băng streak");
    }

    @Test
    void missingPointsOrAnUnavailableAccessLeaveTheStreakUnchanged() {
        streaks.put(user, streak(3, today));
        int savesBefore = streaks.saves;
        when(access.debit(any(), anyInt(), anyString(), any(), anyString(), anyString()))
                .thenThrow(new InsufficientPointsException())
                .thenThrow(new AccessUnavailableException(503));

        assertThatThrownBy(() -> useCase.buyFreeze(user, UUID.randomUUID())).isInstanceOf(InsufficientPointsException.class);
        assertThatThrownBy(() -> useCase.buyFreeze(user, UUID.randomUUID())).isInstanceOf(AccessUnavailableException.class);

        assertThat(streaks.saves).isEqualTo(savesBefore);
        assertThat(streaks.get(user).freezeAvailable()).isFalse();
    }

    @Test
    void repeatingThePurchaseChargesOnceAndThenReportsTheFreezeAsOwned() {
        streaks.put(user, streak(3, today));
        UUID request = UUID.randomUUID();
        useCase.buyFreeze(user, request);

        assertThatThrownBy(() -> useCase.buyFreeze(user, request))
                .satisfies(error -> assertThat(code(error)).isEqualTo("FREEZE_ALREADY_OWNED"));
        verify(access, times(1)).debit(any(), anyInt(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void readingALapsedStreakStoresItAsLostOnce() {
        streaks.put(user, streak(4, today.minusDays(3)));

        StreakView first = useCase.view(user);
        int savesAfterFirst = streaks.saves;
        StreakView second = useCase.view(user);

        assertThat(first.currentDays()).isZero();
        assertThat(first.lostStreakDays()).isEqualTo(4);
        assertThat(second.currentDays()).isZero();
        assertThat(streaks.saves).isEqualTo(savesAfterFirst);
        verify(lock, times(2)).lock(user);
    }

    @Test
    void anActiveStreakShowsTodayAndTheNextFreezePrice() {
        streaks.put(user, streak(5, today));
        StreakView view = useCase.view(user);
        assertThat(view.currentDays()).isEqualTo(5);
        assertThat(view.activeToday()).isTrue();
        assertThat(view.nextFreezeCost()).isEqualTo(10);
        assertThat(view.freezeAvailable()).isFalse();
    }

    /** Keeps streaks in memory so a purchase and the read after it see the same state. */
    private static final class MemoryStreaks implements StreakRepository {
        private final Map<UUID, StreakState> rows = new HashMap<>();
        int saves;

        void put(UUID userId, StreakState state) { rows.put(userId, state); }
        StreakState get(UUID userId) { return rows.get(userId); }

        @Override public Optional<StreakState> find(UUID userId) { return Optional.ofNullable(rows.get(userId)); }
        @Override public Map<UUID, StreakState> findAll(Collection<UUID> userIds) {
            Map<UUID, StreakState> found = new HashMap<>();
            for (UUID id : userIds) if (rows.containsKey(id)) found.put(id, rows.get(id));
            return found;
        }
        @Override public void save(UUID userId, StreakState state) { rows.put(userId, state); saves++; }
    }
}
