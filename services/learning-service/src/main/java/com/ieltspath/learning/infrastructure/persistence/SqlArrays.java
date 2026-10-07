package com.ieltspath.learning.infrastructure.persistence;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Reads PostgreSQL {@code uuid[]} and {@code text[]} columns holding UUIDs, in array order. */
final class SqlArrays {
    private SqlArrays() {}

    static List<UUID> uuids(Array array) throws SQLException {
        try {
            return Arrays.stream((Object[]) array.getArray()).map(value -> UUID.fromString(value.toString())).toList();
        } finally {
            array.free();
        }
    }
}
