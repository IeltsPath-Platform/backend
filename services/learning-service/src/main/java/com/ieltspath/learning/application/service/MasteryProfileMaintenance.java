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
    private final LearnerLock lock;
    private final TransactionTemplate transaction;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "mastery-profile-maintenance");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean();

    public MasteryProfileMaintenance(MasteryProfileRepository profiles, MasteryProfileUpdater updater,
                                     MasteryRulesProvider rules, LearnerLock lock,
                                     PlatformTransactionManager transactionManager) {
        this.profiles = profiles;
        this.updater = updater;
        this.rules = rules;
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

    /** Applies the rules in force now to every stored profile; the score and evidence are not touched. */
    public void recomputeAll() {
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
