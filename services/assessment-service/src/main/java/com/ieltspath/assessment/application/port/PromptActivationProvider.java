package com.ieltspath.assessment.application.port;

/** The prompt version the admin activated for a task, and the model settings of the run that passed. */
public interface PromptActivationProvider {
    String activatedVersion(String task);

    /**
     * Null when the task was never activated; {@code model} and {@code reasoningEffort} are null when the activation
     * does not record them. Implementations that store the run's settings override this.
     */
    default Activation activation(String task) {
        String version = activatedVersion(task);
        return version == null ? null : new Activation(version, null, null);
    }

    record Activation(String promptVersion, String model, String reasoningEffort) {}
}
