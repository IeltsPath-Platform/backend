package com.ieltspath.access.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxEventTest {

    @Test
    void relayedTypesAreStoredAsPending() {
        OutboxEvent event = OutboxEvent.create(UUID.randomUUID(), "Subscription", UUID.randomUUID(), "SubscriptionGranted", "{}");

        assertEquals("PENDING", event.getStatus());
    }

    @Test
    void nonRelayedTypesAreStoredAsNotRelayed() {
        OutboxEvent debit = OutboxEvent.create("PointWallet", UUID.randomUUID(), "PointDebited", "{}");
        OutboxEvent credit = OutboxEvent.create(UUID.randomUUID(), "HumanGrading", UUID.randomUUID(),
                "HumanGradingCreditConsumed", "{}");

        assertEquals("NOT_RELAYED", debit.getStatus());
        assertEquals("NOT_RELAYED", credit.getStatus());
    }

    @Test
    void theCallerSuppliedIdIsKept() {
        UUID id = UUID.randomUUID();

        assertEquals(id, OutboxEvent.create(id, "Subscription", UUID.randomUUID(), "SubscriptionGranted", "{}").getId());
    }
}
