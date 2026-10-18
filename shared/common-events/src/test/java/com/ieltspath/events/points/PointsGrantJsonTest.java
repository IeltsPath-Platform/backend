package com.ieltspath.events.points;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PointsGrantJsonTest {
    private final ObjectMapper json = new ObjectMapper();
    private final UUID grant = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private final UUID user = UUID.fromString("20000000-0000-4000-8000-000000000002");

    @Test
    void requestedGrantKeepsItsFieldNamesValuesAndOrder() throws Exception {
        var event = new PointsGrantRequested(grant, user, 5, "DAILY_QUEST", "quest:1");
        byte[] body = json.writeValueAsBytes(event);
        assertEquals(json.readTree("""
                {"grantId":"20000000-0000-4000-8000-000000000001",
                 "userId":"20000000-0000-4000-8000-000000000002",
                 "amount":5,"reason":"DAILY_QUEST","referenceId":"quest:1"}
                """), json.readTree(body));
        var fields = new java.util.ArrayList<String>();
        json.readTree(body).fieldNames().forEachRemaining(fields::add);
        assertEquals(List.of("grantId", "userId", "amount", "reason", "referenceId"), fields);
        assertEquals(event, json.readValue(body, PointsGrantRequested.class));
    }

    @Test
    void grantedSettlementKeepsTheExplicitNullError() throws Exception {
        var event = PointsGrantSettled.granted(grant);
        byte[] body = json.writeValueAsBytes(event);
        assertEquals(json.readTree("""
                {"grantId":"20000000-0000-4000-8000-000000000001","status":"GRANTED","error":null}
                """), json.readTree(body));
        assertEquals(event, json.readValue(body, PointsGrantSettled.class));
    }

    @Test
    void settlementFactoriesPreserveEveryAcceptedStatusAndFailure() throws Exception {
        assertEquals(Set.of("GRANTED", "CAPPED", "FAILED"), PointsGrantSettled.STATUSES);
        assertEquals(new PointsGrantSettled(grant, "CAPPED", null), PointsGrantSettled.capped(grant));
        var failed = PointsGrantSettled.failed(grant, "INVALID_PAYLOAD");
        assertEquals(json.readTree("""
                {"grantId":"20000000-0000-4000-8000-000000000001","status":"FAILED","error":"INVALID_PAYLOAD"}
                """), json.valueToTree(failed));
        assertEquals(failed, json.readValue(json.writeValueAsBytes(failed), PointsGrantSettled.class));
    }
}
