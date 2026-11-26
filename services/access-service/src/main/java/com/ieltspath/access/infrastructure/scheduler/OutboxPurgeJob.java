package com.ieltspath.access.infrastructure.scheduler;

import com.ieltspath.access.infrastructure.persistence.OutboxPurgeRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OutboxPurgeJob {
    private static final Logger log = LoggerFactory.getLogger(OutboxPurgeJob.class);
    private static final String SERVICE = "access";

    private final OutboxPurgeRepository repository;
    private final MeterRegistry meters;
    private final boolean enabled;
    private final int retentionDays;
    private final int batchSize;
    private final int maxBatchesPerRun;

    public OutboxPurgeJob(OutboxPurgeRepository repository, MeterRegistry meters,
                          @Value("${access.outbox.purge.enabled:true}") boolean enabled,
                          @Value("${access.outbox.purge.retention-days:30}") int retentionDays,
                          @Value("${access.outbox.purge.batch-size:1000}") int batchSize,
                          @Value("${access.outbox.purge.max-batches-per-run:20}") int maxBatchesPerRun) {
        if (retentionDays < 2) throw new IllegalArgumentException("outbox purge retention-days must be at least 2");
        if (batchSize < 1) throw new IllegalArgumentException("outbox purge batch-size must be positive");
        if (maxBatchesPerRun < 1) throw new IllegalArgumentException("outbox purge max-batches-per-run must be positive");
        this.repository = repository;
        this.meters = meters;
        this.enabled = enabled;
        this.retentionDays = retentionDays;
        this.batchSize = batchSize;
        this.maxBatchesPerRun = maxBatchesPerRun;
    }

    @Scheduled(cron = "${access.outbox.purge.cron:0 47 3 * * *}")
    public void purge() {
        if (!enabled) return;
        Instant cutoff = Instant.now().minusSeconds(retentionDays * 86_400L);
        int totalDeleted = 0;
        try {
            for (int batch = 0; batch < maxBatchesPerRun; batch++) {
                int deleted = repository.deletePublishedBefore(cutoff, batchSize);
                totalDeleted += deleted;
                if (deleted < batchSize) break;
            }
            log.info("Outbox purge completed service={} deleted={} retentionDays={}", SERVICE, totalDeleted, retentionDays);
        } catch (RuntimeException failure) {
            log.warn("Outbox purge failed service={} deleted={} errorType={}", SERVICE, totalDeleted,
                    failure.getClass().getSimpleName());
        }
        meters.counter("outbox.purge.deleted", "service", SERVICE).increment(totalDeleted);
    }
}
