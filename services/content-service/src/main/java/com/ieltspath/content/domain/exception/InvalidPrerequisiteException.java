package com.ieltspath.content.domain.exception;

import java.util.List;
import java.util.UUID;

public class InvalidPrerequisiteException extends ContentDomainException {
    private final List<UUID> invalidIds;

    public InvalidPrerequisiteException(List<UUID> invalidIds) {
        super("Có điểm kiến thức tiên quyết không hợp lệ");
        this.invalidIds = List.copyOf(invalidIds);
    }

    public List<UUID> getInvalidIds() {
        return invalidIds;
    }
}
