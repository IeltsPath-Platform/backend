package com.ieltspath.content.application.result;

import java.util.Map;

public record CodeContext(Map<String, String> values, String scopeKey, boolean courseResolved) {
    public CodeContext {
        values = Map.copyOf(values);
    }
}
