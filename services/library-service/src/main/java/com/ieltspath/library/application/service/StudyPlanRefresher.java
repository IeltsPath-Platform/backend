package com.ieltspath.library.application.service;

import com.ieltspath.library.application.port.StudyAccessClient;
import com.ieltspath.library.application.port.StudyGamificationRepository;
import com.ieltspath.library.application.port.StudyGamificationRepository.Profile;
import com.ieltspath.library.application.port.StudyLock;
import com.ieltspath.library.config.StudyGamificationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StudyPlanRefresher {
    private final StudyGamificationRepository repository;
    private final StudyLock lock;
    private final StudyAccessClient access;
    private final StudyGamificationProperties properties;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> inFlight = new ConcurrentHashMap<>();

    public StudyPlanRefresher(StudyGamificationRepository repository, StudyLock lock, StudyAccessClient access,
                              StudyGamificationProperties properties, Clock clock, PlatformTransactionManager manager) {
        this.repository = repository;
        this.lock = lock;
        this.access = access;
        this.properties = properties;
        this.clock = clock;
        this.transactions = new TransactionTemplate(manager);
    }

    public void ensureFresh(UUID user) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Study plan refresh must run before opening a transaction");
        }
        if (fresh(repository.profile(user).orElse(null), clock.instant())) return;

        var completion = new CompletableFuture<Void>();
        var existing = inFlight.putIfAbsent(user, completion);
        if (existing != null) {
            try {
                existing.join();
            } catch (CompletionException failure) {
                if (failure.getCause() instanceof RuntimeException cause) throw cause;
                throw failure;
            }
            return;
        }
        try {
            // A previous request may have finished between the initial read and registration above.
            if (!fresh(repository.profile(user).orElse(null), clock.instant())) refresh(user);
            completion.complete(null);
        } catch (RuntimeException | Error failure) {
            completion.completeExceptionally(failure);
            throw failure;
        } finally {
            inFlight.remove(user, completion);
        }
    }

    private void refresh(UUID user) {
        boolean premium;
        try {
            // Runs on the request thread with its JWT, before any transaction or advisory lock.
            premium = access.premium();
        } catch (StudyAccessClient.AccessUnavailableException unavailable) {
            return;
        }
        int maximum = premium ? properties.getHearts().getPremiumMax() : properties.getHearts().getMax();
        transactions.executeWithoutResult(status -> {
            lock.lock(user);
            Instant now = clock.instant();
            var current = repository.profile(user).orElse(null);
            if (fresh(current, now)) return;
            if (current == null) {
                repository.createProfile(user, maximum, now, properties.getDefaultTimezone());
                current = repository.profile(user).orElseThrow(() -> new IllegalStateException("Study profile was not created"));
            }
            var hearts = current.hearts();
            hearts.regen(now);
            hearts.setMax(maximum, now);
            repository.saveProfile(new Profile(user, current.timezone(), current.totalXp(), current.level(), hearts, now), now);
        });
    }

    private boolean fresh(Profile profile, Instant now) {
        return profile != null && profile.maxHeartsCheckedAt() != null
                && profile.maxHeartsCheckedAt().plus(Duration.ofMinutes(properties.getHearts().getPlanRefreshMinutes())).isAfter(now);
    }
}
