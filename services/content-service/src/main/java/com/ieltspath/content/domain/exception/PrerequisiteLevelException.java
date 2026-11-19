package com.ieltspath.content.domain.exception;

import java.util.List;
import java.util.UUID;

/** A prerequisite edge would run from a higher CEFR level down to a lower one. */
public class PrerequisiteLevelException extends ContentDomainException {
    private final List<UUID> invalidIds;

    public PrerequisiteLevelException(List<UUID> invalidIds) {
        super("Điểm kiến thức tiên quyết không được có cấp CEFR cao hơn điểm phụ thuộc vào nó");
        this.invalidIds = List.copyOf(invalidIds);
    }

    public List<UUID> getInvalidIds() {
        return invalidIds;
    }
}
