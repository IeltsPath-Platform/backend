package com.ieltspath.library.application.port;

import com.ieltspath.library.application.query.NotebookQuery;
import com.ieltspath.library.application.result.NotebookEntryResult;
import com.ieltspath.library.application.result.NotebookSummaryResult;
import com.ieltspath.library.application.result.PageResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VocabularyNotebookReader {
    PageResult<NotebookEntryResult> findEntries(UUID userId, NotebookQuery query, Instant now);
    java.util.Optional<NotebookEntryResult> findEntry(UUID userId, UUID flashcardId);
    NotebookSummaryResult summarize(UUID userId, Instant now, Instant today, Instant tomorrow);
    List<UUID> findSavedSenseIds(UUID userId, Collection<UUID> senseIds);
}
