package com.group01.game.application.port;

import java.util.Map;

public interface OutboxWriter {
    void append(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload);
}
