package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.service.MasteryPolicy;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background jobs over all mastery profiles: filling profiles for evidence that predates them, and re-tiering every
 * profile after Admin changed the rules. One job runs at a time; each learner or batch commits on its own.
 */
@Slf4j
@Component
public class MasteryProfileMaintenance {
    static final int BATCH_SIZE = 500;

    private final MasteryProfileRepository profiles;
    private final MasteryProfileUpdater updater;
    private final MasteryRulesProvider rules;
    private final MasteryFormulaSettings formulas;
    private final LearnerLock lock;
    private final TransactionTemplate transaction;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "mastery-profile-maintenance");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean();

    public MasteryProfileMaintenance(MasteryProfileRepository profiles, MasteryProfileUpdater updater,
                                     MasteryRulesProvider rules, MasteryFormulaSettings formulas, LearnerLock lock,
                                     PlatformTransactionManager transactionManager) {
        this.profiles = profiles;
        this.updater = updater;
        this.rules = rules;
        this.formulas = formulas;
        this.lock = lock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** Starts the fill in the background. Returns false when another job is already running. */
    public boolean startBackfill() {
        return start("backfill", this::backfill);
    }

    /** Starts the re-tiering in the background. Returns false when another job is already running. */
    public boolean startRecompute() {
        return start("recompute", this::recomputeAll);
    }

    /** Builds a profile for every learner and point that has evidence but none yet. Safe to run again. */
    public void backfill() {
        int learners = 0;
        for (UUID userId : profiles.usersWithUnprofiledEvidence()) {
            try {
                transaction.executeWithoutResult(status -> {
                    lock.lock(userId);
                    Set<UUID> missing = profiles.unprofiledKnowledgePoints(userId);
                    updater.rebuild(userId, missing);
                });
                learners++;
            } catch (RuntimeException exception) {
                log.warn("Mastery profile backfill failed for one learner, continuing: userId={}", userId, exception);
            }
        }
        log.info("Mastery profile backfill finished: learners={}", learners);
    }

    /** Starts a rebuild of every profile from its evidence. Returns false when another job is already running. */
    public boolean startRebuild() {
        return start("rebuild", this::rebuildAll);
    }

    /**
     * Rebuilds every stored profile from its evidence under the formula in force: the way to move all learners onto a
     * different formula. Each learner commits on its own, so a failure leaves the others done.
     */
    public void rebuildAll() {
        UUID afterUser = null;
        UUID afterPoint = null;
        int learners = 0;
        while (true) {
            List<MasteryProfile> batch = profiles.findBatchAfter(afterUser, afterPoint, BATCH_SIZE);
            if (batch.isEmpty()) break;
            learners += forEachLearner(batch, updater::rebuild);
            MasteryProfile last = batch.get(batch.size() - 1);
            afterUser = last.userId();
            afterPoint = last.knowledgePointId();
        }
        log.info("Mastery profile rebuild finished: learners={}", learners);
    }

    /**
     * MP-1.0 scores fall as evidence ages even when the learner does not return. Recomputes the profiles whose newest
     * evidence is older than {@code staleAfter}, in batches. Does nothing under the other formula. Safe to run again.
     */
    public int decayStale(java.time.Duration staleAfter) {
        if (!formulas.usesMp1()) return 0;
        java.time.Instant staleBefore = java.time.Instant.now().minus(staleAfter);
        UUID afterUser = null;
        UUID afterPoint = null;
        int profilesSeen = 0;
        while (true) {
            List<MasteryProfile> batch = profiles.findStaleBatchAfter(staleBefore, afterUser, afterPoint, BATCH_SIZE);
            if (batch.isEmpty()) break;
            forEachLearner(batch, updater::decay);
            profilesSeen += batch.size();
            MasteryProfile last = batch.get(batch.size() - 1);
            afterUser = last.userId();
            afterPoint = last.knowledgePointId();
        }
        log.info("Mastery decay finished: profiles={}", profilesSeen);
        return profilesSeen;
    }

    /** Groups a batch by learner and runs the action for each in its own locked transaction; returns learners done. */
    private int forEachLearner(List<MasteryProfile> batch,
                               java.util.function.BiConsumer<UUID, Set<UUID>> action) {
        java.util.Map<UUID, Set<UUID>> byLearner = new java.util.LinkedHashMap<>();
        for (MasteryProfile profile : batch) {
            byLearner.computeIfAbsent(profile.userId(), ignored -> new java.util.HashSet<>())
                    .add(profile.knowledgePointId());
        }
        int done = 0;
        for (var entry : byLearner.entrySet()) {
            try {
                transaction.executeWithoutResult(status -> {
                    lock.lock(entry.getKey());
                    action.accept(entry.getKey(), entry.getValue());
                });
                done++;
            } catch (RuntimeException exception) {
                log.warn("Mastery maintenance failed for one learner, continuing: userId={}", entry.getKey(), exception);
            }
        }
        return done;
    }

    /**
     * Applies the rules in force now to every stored profile. Under MP-1.0 that means rebuilding the scores, because
     * the formula decides them; otherwise only tier and review date change and the score and evidence are not touched.
     */
    public void recomputeAll() {
        if (formulas.usesMp1()) {
            rebuildAll();
            return;
        }
        MasteryPolicy policy = new MasteryPolicy(rules.current());
        UUID afterUser = null;
        UUID afterPoint = null;
        int changedCount = 0;
        while (true) {
            List<MasteryProfile> batch = profiles.findBatchAfter(afterUser, afterPoint, BATCH_SIZE);
            if (batch.isEmpty()) break;
            List<MasteryProfile> changed = batch.stream().filter(profile -> !profile.under(policy).equals(profile))
                    .map(profile -> profile.under(policy)).toList();
            transaction.executeWithoutResult(status -> profiles.reclassify(changed));
            changedCount += changed.size();
            MasteryProfile last = batch.get(batch.size() - 1);
            afterUser = last.userId();
            afterPoint = last.knowledgePointId();
        }
        log.info("Mastery profile recompute finished: changed={}", changedCount);
    }

    private boolean start(String name, Runnable job) {
        if (!running.compareAndSet(false, true)) return false;
        try {
            worker.execute(() -> {
                try {
                    job.run();
                } catch (RuntimeException exception) {
                    log.error("Mastery profile {} failed", name, exception);
                } finally {
                    running.set(false);
                }
            });
        } catch (RuntimeException exception) {
            running.set(false);
            throw exception;
        }
        return true;
    }

    @PreDestroy
    void stop() {
        worker.shutdownNow();
    }
}
