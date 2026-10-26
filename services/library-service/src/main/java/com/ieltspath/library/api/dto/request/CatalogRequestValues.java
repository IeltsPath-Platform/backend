package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.vo.CefrLevel;

import java.util.UUID;

/** Admin forms send an empty string for an unset choice; these helpers turn it into "no value". */
final class CatalogRequestValues {
    static final String UUID_OR_EMPTY = "^$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    static final String CEFR_OR_EMPTY = "^$|^(?i)(A1|A2|B1|B2|C1|C2)$";

    private CatalogRequestValues() {
    }

    static UUID uuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value.trim());
    }

    static CefrLevel cefr(String value) {
        return value == null || value.isBlank() ? null : CefrLevel.valueOf(value.trim().toUpperCase());
    }
}
