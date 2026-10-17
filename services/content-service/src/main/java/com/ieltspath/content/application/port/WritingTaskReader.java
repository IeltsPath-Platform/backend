package com.ieltspath.content.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reads the writing prompt of one section of a published practice test, with its chart images and writing guide. */
public interface WritingTaskReader {
    Optional<WritingTask> find(UUID packageVersionId, UUID sectionId);

    /**
     * {@code guide} is the Markdown text shown beside the prompt; {@code guideSteps} the JSON array of the step-by-step
     * mode. Both are null when the prompt has none.
     */
    record WritingTask(UUID packageId, UUID packageVersionId, UUID sectionId, String packageTitle, String title,
                       Integer partNo, Integer timeLimitSeconds, String instructions, UUID questionVersionId,
                       String stem, String task, Integer minWords, String guide, String guideSteps,
                       List<Image> images) {}

    record Image(String mediaReference, String altText) {}
}
