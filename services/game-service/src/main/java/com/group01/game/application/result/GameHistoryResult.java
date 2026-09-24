package com.group01.game.application.result;

import java.util.List;

public record GameHistoryResult(List<GameSessionResult> content, int page, int size,
                                long totalElements, long totalPages) {
    public GameHistoryResult {
        content = List.copyOf(content);
    }
}
