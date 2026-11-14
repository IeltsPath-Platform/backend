package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.LearningRuleConfigRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.vo.MasteryTier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Background jobs over every profile: one at a time, one learner per transaction, in batches. */
class MasteryProfileMaintenanceTest {
    private static final Instant LAST = Instant.parse("2026-10-01T00:00:00Z");
    private final MasteryProfileRepository profiles = mock(MasteryProfileRepository.class);
    private final MasteryProfileUpdater updater = mock(MasteryProfileUpdater.class);
    private final LearningRuleConfigRepository configs = mock(LearningRuleConfigRepository.class);
    private final LearnerLock lock = mock(LearnerLock.class);
    private final MasteryRulesProvider rules =
            new MasteryRulesProvider(configs, new ObjectMapper(), 0.6, 0.8, List.of(7, 14, 30, 45, 60), 3);
    private final PlatformTransactionManager transactions = new PlatformTransactionManager() {
        @Override public TransactionStatus getTransaction(TransactionDefinition definition) { return new SimpleTransactionStatus(); }
        @Override public void commit(TransactionStatus status) { }
        @Override public void rollback(TransactionStatus status) { }
    };

    MasteryProfileMaintenanceTest() {
        when(configs.findAll()).thenReturn(Map.of());
    }

    private MasteryProfileMaintenance maintenance(boolean mp1) {
        MasteryFormulaSettings formulas = new MasteryFormulaSettings(configs, new ObjectMapper(), mp1 ? "MP1" : "DEEPTUTOR",
                8, 3, 30, 3, 2, 1.0, 0.03, 7, 1.0, 1.0, 1.0, 0.6, 0.5, 0.3, 0.75);
        return new MasteryProfileMaintenance(profiles, updater, rules, formulas, lock, transactions);
    }

    private static MasteryProfile profile(UUID user, double mastery, MasteryTier tier) {
        return new MasteryProfile(user, UUID.randomUUID(), mastery, tier, 5, 1, LAST, null, null, List.of());
    }

    @Test
    void onlyOneJobRunsAtATime() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(profiles.usersWithUnprofiledEvidence()).thenAnswer(call -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
            return List.of();
        });
        MasteryProfileMaintenance maintenance = maintenance(true);

        assertThat(maintenance.startBackfill()).isTrue();
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(maintenance.startRecompute()).isFalse();
        assertThat(maintenance.startRebuild()).isFalse();
        assertThat(maintenance.startBackfill()).isFalse();
        release.countDown();

        boolean freed = false;
        for (int attempt = 0; attempt < 200 && !freed; attempt++) {
            freed = maintenance.startBackfill();
            if (!freed) Thread.sleep(10);
        }
        assertThat(freed).as("a new job may start once the first has finished").isTrue();
    }

    @Test
    void aFailureForOneLearnerDoesNotStopTheOthers() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        List<MasteryProfile> batch = List.of(profile(a, 0.5, MasteryTier.PRIORITY), profile(b, 0.5, MasteryTier.PRIORITY),
                profile(c, 0.5, MasteryTier.PRIORITY));
        when(profiles.findBatchAfter(any(), any(), eq(MasteryProfileMaintenance.BATCH_SIZE))).thenReturn(batch, List.of());
        doThrow(new IllegalStateException("broken row")).when(updater).rebuild(eq(b), any());

        maintenance(true).rebuildAll();

        verify(updater).rebuild(eq(a), any());
        verify(updater).rebuild(eq(c), any());
        verify(lock).lock(a);
        verify(lock).lock(b);
        verify(lock).lock(c);
    }

    @Test
    void underMp1ARecomputeRebuildsEveryProfileFromItsEvidence() {
        UUID a = UUID.randomUUID();
        MasteryProfile stored = profile(a, 0.7, MasteryTier.CONSOLIDATE);
        when(profiles.findBatchAfter(any(), any(), eq(MasteryProfileMaintenance.BATCH_SIZE))).thenReturn(List.of(stored), List.of());

        maintenance(true).recomputeAll();

        verify(updater).rebuild(a, Set.of(stored.knowledgePointId()));
        verify(profiles, never()).reclassify(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void underDeepTutorARecomputeWritesOnlyTheProfilesWhoseTierChanges() {
        UUID a = UUID.randomUUID();
        when(configs.findAll()).thenReturn(Map.of(MasteryRulesProvider.PRIORITY_BELOW, "0.75"));
        MasteryProfile moves = new MasteryProfile(a, UUID.randomUUID(), 0.7, MasteryTier.CONSOLIDATE, 5, 1, LAST,
                LAST.plus(Duration.ofDays(3)), null, List.of());
        MasteryProfile stays = new MasteryProfile(a, UUID.randomUUID(), 0.9, MasteryTier.MAINTAIN, 5, 1, LAST,
                LAST.plus(Duration.ofDays(14)), null, List.of());
        when(profiles.findBatchAfter(any(), any(), eq(MasteryProfileMaintenance.BATCH_SIZE))).thenReturn(List.of(moves, stays), List.of());

        maintenance(false).recomputeAll();

        ArgumentCaptor<List<MasteryProfile>> written = ArgumentCaptor.forClass(List.class);
        verify(profiles).reclassify(written.capture());
        assertThat(written.getValue()).singleElement().satisfies(profile -> {
            assertThat(profile.knowledgePointId()).isEqualTo(moves.knowledgePointId());
            assertThat(profile.tier()).isEqualTo(MasteryTier.PRIORITY);
            assertThat(profile.mastery()).isEqualTo(0.7);
        });
        verify(updater, never()).rebuild(any(), any());
    }

    @Test
    void decayUnderDeepTutorReadsNothing() {
        assertThat(maintenance(false).decayStale(Duration.ofHours(24))).isZero();
        verify(profiles, never()).findStaleBatchAfter(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
        verify(updater, never()).decay(any(), any());
    }

    @Test
    void decayPagesThroughEveryStaleProfileInBatches() {
        UUID user = UUID.randomUUID();
        List<MasteryProfile> full = new ArrayList<>();
        for (int i = 0; i < 500; i++) full.add(profile(user, 0.5, MasteryTier.PRIORITY));
        List<MasteryProfile> secondFull = new ArrayList<>();
        for (int i = 0; i < 500; i++) secondFull.add(profile(user, 0.5, MasteryTier.PRIORITY));
        when(profiles.findStaleBatchAfter(any(), any(), any(), eq(MasteryProfileMaintenance.BATCH_SIZE)))
                .thenReturn(full, secondFull, List.of(profile(user, 0.5, MasteryTier.PRIORITY)), List.of());

        int seen = maintenance(true).decayStale(Duration.ofHours(24));

        assertThat(seen).isEqualTo(1001);
        verify(profiles, times(4)).findStaleBatchAfter(any(), any(), any(), eq(MasteryProfileMaintenance.BATCH_SIZE));
        verify(updater, times(3)).decay(eq(user), any());
    }
}
