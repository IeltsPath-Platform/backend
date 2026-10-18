package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.ProgressMetadataResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class GetProgressMetadataUseCase {
    private final LearningContentReader reader;
    public GetProgressMetadataUseCase(LearningContentReader reader) { this.reader = reader; }

    @Transactional(readOnly = true)
    public ProgressMetadataResult execute(List<UUID> courseIds) {
        if (courseIds == null || courseIds.isEmpty() || courseIds.size() > 100 || courseIds.stream().anyMatch(java.util.Objects::isNull))
            throw new IllegalArgumentException("Between 1 and 100 course ids are required");
        return reader.progressMetadata(courseIds.stream().distinct().toList());
    }
}
