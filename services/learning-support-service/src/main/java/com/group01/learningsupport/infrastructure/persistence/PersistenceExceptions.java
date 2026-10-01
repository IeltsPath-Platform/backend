package com.group01.learningsupport.infrastructure.persistence;

import com.group01.learningsupport.domain.exception.InvalidDataException;

import com.group01.learningsupport.domain.exception.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

public final class PersistenceExceptions {
    private PersistenceExceptions() {
    }

    public static RuntimeException translate(RuntimeException exception) {
        String state = sqlState(exception);
        if ("23505".equals(state)) {
            return new ConflictException();
        }
        if ("23514".equals(state) || "23503".equals(state) || "23502".equals(state)) {
            return new InvalidDataException("Dữ liệu không hợp lệ");
        }
        if (exception instanceof DataIntegrityViolationException) {
            return exception;
        }
        return exception;
    }

    public static RuntimeException translate(DataIntegrityViolationException exception) {
        return translate((RuntimeException) exception);
    }

    private static String sqlState(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                return sqlException.getSQLState();
            }
            current = current.getCause();
        }
        return "";
    }
}
