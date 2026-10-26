package com.ieltspath.library.domain.catalog;

import com.ieltspath.library.domain.exception.InvalidDataException;

/** Text rules shared by catalog values: blank means absent, and every value fits its column. */
final class CatalogText {
    private CatalogText() {
    }

    static String optional(String value, int max, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new InvalidDataException(field + " must not exceed " + max + " characters");
        }
        return trimmed;
    }

    static String required(String value, int max, String field) {
        String text = optional(value, max, field);
        if (text == null) {
            throw new InvalidDataException(field + " is required");
        }
        return text;
    }

    /** Covers are stored as object keys; a full URL would bypass the storage that resolves them. */
    static String coverKey(String value) {
        String key = optional(value, 500, "coverKey");
        if (key != null && key.regionMatches(true, 0, "http", 0, 4)) {
            throw new InvalidDataException("coverKey must be an object key, not a URL");
        }
        return key;
    }

    static int sortOrder(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new InvalidDataException("sortOrder must not be negative");
        }
        return value;
    }
}
