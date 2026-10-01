package com.group01.learningsupport.domain;

import com.group01.learningsupport.domain.exception.InvalidDataException;

import java.util.UUID;

public final class DomainChecks {
    public static final int DESCRIPTION_MAX = 2_000;
    public static final int SEGMENT_NOTE_MAX = 2_000;
    public static final int HIGHLIGHT_MAX = 2_000;
    public static final int FRONT_MAX = 4_000;
    public static final int BODY_MAX = 20_000;
    public static final int TRANSCRIPT_MAX = 20_000;

    private DomainChecks() {
    }

    public static UUID userId(UUID userId) {
        if (userId == null) {
            throw new InvalidDataException("userId không hợp lệ");
        }
        return userId;
    }

    public static String required(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new InvalidDataException(field + " không hợp lệ");
        }
        return value.trim();
    }

    public static String optional(String value, int max, String field) {
        if (value == null) {
            return null;
        }
        if (value.length() > max) {
            throw new InvalidDataException(field + " không hợp lệ");
        }
        return value;
    }

    public static int nonNegative(Integer value, String field) {
        if (value == null || value < 0) {
            throw new InvalidDataException(field + " không hợp lệ");
        }
        return value;
    }
}
