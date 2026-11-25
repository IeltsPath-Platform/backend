package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.ContentOverviewReader;
import com.ieltspath.content.application.result.ContentOverviewResult;
import com.ieltspath.content.application.result.ContentOverviewResult.QuestionLabel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetContentOverviewUseCase {
    /** The most labels one call gives; the overview asks for a handful. */
    public static final int MAX_LABELS = 50;

    private final ContentOverviewReader reader;

    public GetContentOverviewUseCase(ContentOverviewReader reader) {
        this.reader = reader;
    }

    public ContentOverviewResult execute() {
        return reader.read();
    }

    public List<QuestionLabel> questionLabels(Collection<UUID> questionVersionIds) {
        List<UUID> ids = questionVersionIds.stream().distinct().limit(MAX_LABELS).toList();
        return ids.isEmpty() ? List.of() : reader.questionLabels(ids);
    }
}