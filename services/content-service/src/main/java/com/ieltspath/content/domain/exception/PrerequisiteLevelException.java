package com.ieltspath.content.domain.exception;

import java.util.List;
import java.util.UUID;

/** A prerequisite edge would run from a higher CEFR level down to a lower one. */
public class PrerequisiteLevelException extends ContentDomainException {
    private final List<UUID> invalidIds;

    public PrerequisiteLevelException(List<UUID> invalidIds) {
        super("Prerequisites must not have a higher CEFR level than the point that depends on them: " + invalidIds);
        this.invalidIds = List.copyOf(invalidIds);
    }

    public List<UUID> getInvalidIds() {
        return invalidIds;
    }
}
