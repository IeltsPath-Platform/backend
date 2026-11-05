package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.VocabularyNotebookReader;
import com.ieltspath.library.application.query.NotebookQuery;
import com.ieltspath.library.application.result.NotebookEntryResult;
import com.ieltspath.library.application.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListNotebookEntriesUseCase {
    private final VocabularyNotebookReader reader;
    public PageResult<NotebookEntryResult> execute(UUID userId, NotebookQuery query) {
        return reader.findEntries(userId, query, Instant.now());
    }
}
