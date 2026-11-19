package com.ieltspath.content.domain.exception;

import java.util.List;
import java.util.UUID;

public class PrerequisiteCycleException extends ContentDomainException {
    private final List<UUID> cycle;

    public PrerequisiteCycleException(List<UUID> cycle) {
        super("Điểm kiến thức tiên quyết tạo thành vòng lặp");
        this.cycle = List.copyOf(cycle);
    }

    public List<UUID> getCycle() {
        return cycle;
    }
}
