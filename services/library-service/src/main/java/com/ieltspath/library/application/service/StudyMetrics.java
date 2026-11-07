package com.ieltspath.library.application.service;

import io.micrometer.core.instrument.Metrics;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Operation totals follow durable state transitions, including when a caller owns the transaction. */
final class StudyMetrics {
    private StudyMetrics() {}
    static void afterCommit(String name,String... tags) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {Metrics.counter(name,tags).increment();}
        });
    }
}
