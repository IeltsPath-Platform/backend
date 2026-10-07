package com.ieltspath.content.application.command;

import java.util.List;
import java.util.UUID;

/** Null {@code excludePackageIds} or {@code minQuestions} take their defaults. */
public record CountPracticeSetsCommand(
        List<UUID> knowledgePointIds,
        List<UUID> excludePackageIds,
        Integer minQuestions
) {}
