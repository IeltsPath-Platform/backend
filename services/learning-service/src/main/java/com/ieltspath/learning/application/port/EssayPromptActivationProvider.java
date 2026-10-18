package com.ieltspath.learning.application.port;

public interface EssayPromptActivationProvider {
    /** Null only when Assessment confirms that the task has never had a prompt activation. */
    String activatedVersion(String task);
}
