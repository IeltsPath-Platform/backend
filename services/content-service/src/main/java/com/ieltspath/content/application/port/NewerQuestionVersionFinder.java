package com.ieltspath.content.application.port;

import java.util.Optional;
import java.util.UUID;

/** Finds the newest published version of the same question that comes after a given version. */
public interface NewerQuestionVersionFinder {

    Optional<UUID> newerPublishedVersion(UUID questionVersionId);
}
