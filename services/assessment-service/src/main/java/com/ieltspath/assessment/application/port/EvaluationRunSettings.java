package com.ieltspath.assessment.application.port;

/** What an AI evaluation run needs to know about the grading model and the prompt templates it can use. */
public interface EvaluationRunSettings {
    /** Model name that grades the essays of a run. */
    String model();

    /** Reasoning effort sent to the model; {@code null} when none is configured. */
    String reasoningEffort();

    /** Prompt template a run uses when the caller does not name one. */
    String defaultTemplateVersion();

    boolean isKnownTemplate(String templateVersion);

    /** Version string of the prompt the given rubric and template produce, stored with the run. */
    String promptVersion(ActiveRubricProvider.Rubric rubric, String templateVersion);
}
