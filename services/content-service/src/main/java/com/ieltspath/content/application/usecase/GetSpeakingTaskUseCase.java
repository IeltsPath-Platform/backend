package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.SpeakingTaskReader;
import com.ieltspath.content.domain.exception.PackageVersionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** The questions of one Speaking part for the practice workspace; a Speaking question has no answer to hide. */
@Service
public class GetSpeakingTaskUseCase {
    private final SpeakingTaskReader reader;

    public GetSpeakingTaskUseCase(SpeakingTaskReader reader) { this.reader = reader; }

    @Transactional(readOnly = true)
    public SpeakingTaskReader.SpeakingTask execute(UUID packageVersionId, UUID sectionId) {
        return reader.find(packageVersionId, sectionId)
                .orElseThrow(() -> new PackageVersionNotFoundException(packageVersionId));
    }
}
