package com.ieltspath.user.api.exception;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void badRequestHasTheStandardErrorFieldNames() throws Exception {
        assertFieldNamesForStatus(400);
    }

    @Test
    void notFoundHasTheStandardErrorFieldNames() throws Exception {
        assertFieldNamesForStatus(404);
    }

    private void assertFieldNamesForStatus(int status) throws Exception {
        RecordComponent[] components = ErrorResponse.class.getRecordComponents();
        assertThat(java.util.Arrays.stream(components).map(RecordComponent::getName))
                .contains("timestamp", "status", "error", "message", "path", "details", "code");
        Object[] values = IntStream.range(0, components.length)
                .mapToObj(index -> valueFor(components[index], status))
                .toArray();
        ErrorResponse response = (ErrorResponse) ErrorResponse.class
                .getDeclaredConstructor(java.util.Arrays.stream(components)
                        .map(RecordComponent::getType).toArray(Class[]::new))
                .newInstance(values);
        Set<String> fieldNames = objectMapper.valueToTree(response).properties()
                .stream().map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());

        assertThat(fieldNames).contains("timestamp", "status", "error", "message", "path", "details", "code");
    }

    private Object valueFor(RecordComponent component, int status) {
        Class<?> type = component.getType();
        if (type == int.class || type == Integer.class) return status;
        if (type == Instant.class) return Instant.parse("2026-11-01T00:00:00Z");
        if (type == LocalDateTime.class) return LocalDateTime.parse("2026-11-01T00:00:00");
        if (Map.class.isAssignableFrom(type)) return new LinkedHashMap<String, String>();
        if (type == String.class) return "test";
        return null;
    }
}
