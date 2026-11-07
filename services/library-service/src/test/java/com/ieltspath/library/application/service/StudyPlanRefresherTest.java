package com.ieltspath.library.application.service;

import com.ieltspath.library.application.port.StudyAccessClient;
import com.ieltspath.library.application.port.StudyGamificationRepository;
import com.ieltspath.library.application.port.StudyGamificationRepository.Profile;
import com.ieltspath.library.application.port.StudyLock;
import com.ieltspath.library.config.StudyGamificationProperties;
import com.ieltspath.library.domain.study.StudyHearts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StudyPlanRefresherTest {
    private final UUID user = UUID.randomUUID();
    private final Instant started = Instant.parse("2026-11-07T17:30:00Z");
    private final AtomicReference<Instant> now = new AtomicReference<>(started);
    private final AtomicReference<Profile> stored = new AtomicReference<>();
    private final StudyGamificationRepository repository = mock(StudyGamificationRepository.class);
    private final StudyLock lock = mock(StudyLock.class);
    private final StudyAccessClient access = mock(StudyAccessClient.class);
    private final StudyGamificationProperties properties = new StudyGamificationProperties();
    private final DataSource source = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private DataSourceTransactionManager manager;
    private StudyPlanRefresher refresher;

    @BeforeEach
    void setup() throws Exception {
        when(source.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        manager = new DataSourceTransactionManager(source);
        Clock clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now.get(); }
        };
        when(repository.profile(user)).thenAnswer(ignored -> Optional.ofNullable(stored.get()));
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive(), "StudyLock must run inside the short update transaction");
            return null;
        }).when(lock).lock(user);
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            int max = call.getArgument(1);
            Instant at = call.getArgument(2);
            String timezone = call.getArgument(3);
            stored.compareAndSet(null, new Profile(user, timezone, 0, 1,
                    StudyHearts.start(user, max, at, Duration.ofHours(5)), null));
            return null;
        }).when(repository).createProfile(eq(user), anyInt(), any(), anyString());
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            stored.set(call.getArgument(0));
            return null;
        }).when(repository).saveProfile(any(), any());
        refresher = new StudyPlanRefresher(repository, lock, access, properties, clock, manager);
    }

    @Test
    void newPremiumLearnerStartsWithSevenFullHeartsAndSuccessfulTimestamp() throws Exception {
        when(access.premium()).thenAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return true;
        });
        refresher.ensureFresh(user);
        assertEquals(7, stored.get().hearts().hearts());
        assertEquals(7, stored.get().hearts().max());
        assertEquals(started, stored.get().maxHeartsCheckedAt());
        assertEquals("Asia/Ho_Chi_Minh", stored.get().timezone());
        var order = inOrder(access, source, lock);
        order.verify(access).premium();
        order.verify(source).getConnection();
        order.verify(lock).lock(user);
    }

    @Test
    void standardPlanIsFiveAndCacheLastsTenMinutesAcrossCalls() {
        when(access.premium()).thenReturn(false);
        for (int i = 0; i < 6; i++) refresher.ensureFresh(user);
        assertEquals(5, stored.get().hearts().hearts());
        assertEquals(5, stored.get().hearts().max());
        verify(access, times(1)).premium();
        now.set(started.plus(Duration.ofMinutes(10)).minusMillis(1));
        refresher.ensureFresh(user);
        verify(access, times(1)).premium();
        now.set(started.plus(Duration.ofMinutes(10)));
        refresher.ensureFresh(user);
        verify(access, times(2)).premium();
        assertEquals(now.get(), stored.get().maxHeartsCheckedAt());
    }

    @Test
    void upgradeKeepsCurrentHeartsAndStartsTheRegenerationClock() {
        profile(5, 5);
        when(access.premium()).thenReturn(true);
        refresher.ensureFresh(user);
        assertEquals(5, stored.get().hearts().hearts());
        assertEquals(7, stored.get().hearts().max());
        assertEquals(started, stored.get().hearts().lastRegenAt());
        assertEquals("Pacific/Auckland", stored.get().timezone());
        assertEquals(502, stored.get().totalXp());
        assertEquals(2, stored.get().level());
    }

    @Test
    void downgradeClampsSevenHeartsButKeepsThreeHearts() {
        when(access.premium()).thenReturn(false);
        for (int hearts : new int[]{7, 3}) {
            profile(hearts, 7);
            refresher.ensureFresh(user);
            assertEquals(Math.min(hearts, 5), stored.get().hearts().hearts());
            assertEquals(5, stored.get().hearts().max());
        }
    }

    @Test
    void accessFailurePreservesTheStoredMaximumAndSuccessfulCheckTimestamp() {
        profile(3, 7);
        var before = stored.get();
        when(access.premium()).thenThrow(new StudyAccessClient.AccessUnavailableException());
        assertDoesNotThrow(() -> refresher.ensureFresh(user));
        assertSame(before, stored.get());
        assertEquals(7, stored.get().hearts().max());
        assertEquals(started.minus(Duration.ofMinutes(11)), stored.get().maxHeartsCheckedAt());
        verifyNoInteractions(source, lock);
        verify(repository, never()).saveProfile(any(), any());
    }

    @Test
    void accessFailureForNewLearnerDoesNotCreateAProfile() {
        when(access.premium()).thenThrow(new StudyAccessClient.AccessUnavailableException());
        assertDoesNotThrow(() -> refresher.ensureFresh(user));
        assertNull(stored.get());
        verify(repository, never()).createProfile(any(), anyInt(), any(), anyString());
        verifyNoInteractions(source, lock);
    }

    @Test
    void refusesAnAlreadyTransactionalCallerBeforeAnyHttpOrRepositoryRead() {
        new TransactionTemplate(manager).executeWithoutResult(status ->
                assertThrows(IllegalStateException.class, () -> refresher.ensureFresh(user)));
        verifyNoInteractions(access, repository, lock);
    }

    @Test
    void concurrentRefreshesShareTheInFlightHttpCallWithoutHoldingStudyLocks() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(access.premium()).thenAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            verifyNoInteractions(lock);
            entered.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return true;
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> refresher.ensureFresh(user));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var second = executor.submit(() -> refresher.ensureFresh(user));
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            verify(access, times(1)).premium();
            assertEquals(7, stored.get().hearts().hearts());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private void profile(int hearts, int max) {
        stored.set(new Profile(user, "Pacific/Auckland", 502, 2,
                StudyHearts.restore(user, hearts, max, started, Duration.ofHours(5)),
                started.minus(Duration.ofMinutes(11))));
    }
}
