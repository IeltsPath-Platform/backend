package com.ieltspath.library.domain.catalog;

import com.ieltspath.library.domain.exception.InvalidDataException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Size limits that keep the child lists of one sense and the membership of one set bounded. */
public final class CatalogLimits {
    public static final int MAX_CHILDREN_PER_SENSE = 30;
    public static final int MAX_SENSES_PER_SET = 500;

    private CatalogLimits() {
    }

    public static <T> List<T> requireChildren(List<T> values) {
        if (values == null) {
            throw new InvalidDataException("entries are required");
        }
        if (values.size() > MAX_CHILDREN_PER_SENSE) {
            throw new InvalidDataException("at most " + MAX_CHILDREN_PER_SENSE + " entries are allowed");
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new InvalidDataException("entries must not contain empty values");
        }
        return List.copyOf(values);
    }

    public static List<UUID> requireSetSenses(List<UUID> senseIds) {
        if (senseIds == null) {
            throw new InvalidDataException("senseIds are required");
        }
        if (senseIds.size() > MAX_SENSES_PER_SET) {
            throw new InvalidDataException("at most " + MAX_SENSES_PER_SET + " senses are allowed");
        }
        if (senseIds.stream().anyMatch(Objects::isNull)) {
            throw new InvalidDataException("senseIds must not contain empty values");
        }
        if (new HashSet<>(senseIds).size() != senseIds.size()) {
            throw new InvalidDataException("senseIds must be unique");
        }
        return List.copyOf(senseIds);
    }
}
