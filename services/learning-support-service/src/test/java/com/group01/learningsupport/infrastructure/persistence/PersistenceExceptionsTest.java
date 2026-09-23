package com.group01.learningsupport.infrastructure.persistence;

import com.group01.learningsupport.domain.exception.InvalidDataException;
import com.group01.learningsupport.domain.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceExceptionsTest {
    @Test
    void uniqueViolationBecomesConflict() {
        RuntimeException translated = PersistenceExceptions.translate(violation("23505"));
        assertInstanceOf(ConflictException.class, translated);
        assertEquals("Dữ liệu đã tồn tại", translated.getMessage());
    }

    @Test
    void checkViolationBecomesInvalidData() {
        RuntimeException translated = PersistenceExceptions.translate(violation("23514"));
        assertInstanceOf(InvalidDataException.class, translated);
        assertEquals("Dữ liệu không hợp lệ", translated.getMessage());
    }

    private static DataIntegrityViolationException violation(String sqlState) {
        return new DataIntegrityViolationException("db", new SQLException("constraint", sqlState));
    }
}
