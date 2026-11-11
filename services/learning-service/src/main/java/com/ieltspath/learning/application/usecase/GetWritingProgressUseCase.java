package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.WritingProgressResult;
import com.ieltspath.learning.application.service.ErrorTypeTexts;
import com.ieltspath.learning.application.service.WritingProgressCalculator;
import com.ieltspath.learning.application.service.WritingSettings;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.WritingProgressEssay;
import com.ieltspath.learning.domain.vo.WritingTask;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reads the learner's graded first essays and summarizes them; no LLM call, no write. */
@Service
public class GetWritingProgressUseCase {
    static final int DEFAULT_LIMIT = 10;
    static final int MIN_LIMIT = 3;
    static final int MAX_LIMIT = 20;

    private final WritingSubmissionRepository essays;
    private final ErrorTypeTexts errorTypeTexts;
    private final WritingSettings settings;

    public GetWritingProgressUseCase(WritingSubmissionRepository essays, ErrorTypeTexts errorTypeTexts,
                                     WritingSettings settings) {
        this.essays = essays;
        this.errorTypeTexts = errorTypeTexts;
        this.settings = settings;
    }

    /**
     * @param task  {@code TASK_1} or {@code TASK_2}; null for both
     * @param limit essays per task, 3 to 20; null for the default
     */
    public WritingProgressResult execute(UUID userId, String task, Integer limit) {
        int window = limit == null ? DEFAULT_LIMIT : limit;
        if (window < MIN_LIMIT || window > MAX_LIMIT) {
            throw new LearningRequestException(400, "INVALID_LIMIT", "limit must be between 3 and 20");
        }
        List<WritingTask> tasks = task == null ? List.of(WritingTask.values()) : List.of(parseTask(task));

        Map<WritingTask, List<WritingProgressEssay>> byTask = new EnumMap<>(WritingTask.class);
        for (WritingTask writingTask : tasks) {
            byTask.put(writingTask, essays.findFirstGradedEssays(userId, writingTask.name(), window));
        }
        // Content is asked once, and only when some stored grade lacks the names it needs.
        Map<String, ErrorTypeTexts.Text> texts = anyNameMissing(byTask) ? errorTypeTexts.byCode() : Map.of();

        WritingProgressCalculator calculator = new WritingProgressCalculator(settings.progressFixedAfter());
        List<WritingProgressResult.TaskProgress> blocks = new ArrayList<>();
        for (WritingTask writingTask : tasks) {
            blocks.add(calculator.calculate(writingTask.name(), byTask.get(writingTask), texts));
        }
        return new WritingProgressResult(blocks);
    }

    private static WritingTask parseTask(String value) {
        return WritingTask.parse(value).orElseThrow(() ->
                new LearningRequestException(400, "INVALID_TASK", "task must be TASK_1 or TASK_2"));
    }

    private static boolean anyNameMissing(Map<WritingTask, List<WritingProgressEssay>> byTask) {
        return byTask.values().stream().flatMap(List::stream)
                .flatMap(essay -> essay.corrections().stream())
                .anyMatch(correction -> correction != null && correction.errorCode() != null
                        && (correction.errorName() == null || correction.fixHint() == null));
    }
}
