package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/** The active courses open to everyone: the learning flow lets their lessons and points past the course paywall. */
@Service
@Transactional(readOnly = true)
public class GetFreeCourseIdsUseCase {
    private final LearningContentReader reader;

    public GetFreeCourseIdsUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public Set<UUID> execute() {
        return reader.freeCourseIds();
    }
}
