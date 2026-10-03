package com.group01.content.application.usecase;

import com.group01.content.application.command.SearchPracticeSetsCommand;
import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.PracticeSetResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Finds practice sets that can serve as review material for one knowledge point. */
@Service
@Transactional(readOnly = true)
public class SearchPracticeSetsUseCase {
    /** A set needs this many questions by default; the one-question demo package of V4 is too small. */
    public static final int DEFAULT_MIN_QUESTIONS = 3;
    static final int DEFAULT_LIMIT = 1;
    static final int MAX_LIMIT = 10;

    private final LearningContentReader reader;

    public SearchPracticeSetsUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public List<PracticeSetResult> execute(SearchPracticeSetsCommand command) {
        if (command.knowledgePointId() == null) {
            throw new IllegalArgumentException("knowledgePointId is required");
        }
        int minQuestions = command.minQuestions() == null ? DEFAULT_MIN_QUESTIONS : command.minQuestions();
        int limit = command.limit() == null ? DEFAULT_LIMIT : command.limit();
        if (minQuestions < 1) {
            throw new IllegalArgumentException("minQuestions must be at least 1");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        List<UUID> exclude = command.excludePackageIds() == null ? List.of() : command.excludePackageIds();
        return reader.searchPracticeSets(command.knowledgePointId(), exclude, minQuestions, limit,
                command.preferredLessonId());
    }
}
