package com.ieltspath.assessment.infrastructure.messaging;

import com.ieltspath.assessment.application.event.OutboxEventsRecorded;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs the relay right after a transaction that recorded urgent outbox rows commits, so the examiner's bell rings within a
 * moment of the learner's choice instead of waiting for the next tick. The timer stays: it is what delivers the rows if this
 * run fails or the process stops in between. Both can run at once; the relay skips rows the other has claimed.
 */
@Component
@ConditionalOnProperty(name = "assessment.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayNudge {
    private final OutboxRelayScheduler relay;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "outbox-relay-nudge");
        thread.setDaemon(true);
        return thread;
    });

    public OutboxRelayNudge(OutboxRelayScheduler relay) {
        this.relay = relay;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecorded(OutboxEventsRecorded recorded) {
        worker.execute(relay::relayPendingEvents);
    }

    @PreDestroy
    void stop() {
        worker.shutdown();
    }
}
