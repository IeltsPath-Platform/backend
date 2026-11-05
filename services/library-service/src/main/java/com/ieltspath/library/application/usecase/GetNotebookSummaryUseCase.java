package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.VocabularyNotebookReader;
import com.ieltspath.library.application.result.NotebookSummaryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetNotebookSummaryUseCase {
    private final VocabularyNotebookReader reader;
    private final Clock clock;
    public NotebookSummaryResult execute(UUID userId, ZoneId zone) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, zone);
        return reader.summarize(userId, now, today.atStartOfDay(zone).toInstant(), today.plusDays(1).atStartOfDay(zone).toInstant());
    }
}
