package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.PlacementHistoryReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetPlacementHistoryUseCase {
    private static final int MAX_PAGE = 100_000;
    private static final int MAX_SIZE = 50;

    private final PlacementHistoryReader reader;

    public GetPlacementHistoryUseCase(PlacementHistoryReader reader) {
        this.reader = reader;
    }

    public record Page(List<PlacementHistoryReader.Item> items, int page, int size, boolean hasNext) {}

    @Transactional(readOnly = true)
    public Page execute(UUID userId, int page, int size) {
        if (page < 0 || page > MAX_PAGE || size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Số trang hoặc kích thước trang lịch sử không hợp lệ.");
        }
        // One row more than asked tells whether an older page exists without a second count query.
        List<PlacementHistoryReader.Item> rows = reader.find(userId, (long) page * size, size + 1);
        boolean hasNext = rows.size() > size;
        return new Page(hasNext ? rows.subList(0, size) : rows, page, size, hasNext);
    }
}
