package com.ieltspath.learning.application.query;

import com.ieltspath.learning.domain.exception.InvalidDataException;

public record PageQuery(int page, int size) {
    public PageQuery {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidDataException("Phân trang không hợp lệ");
        }
    }
}
