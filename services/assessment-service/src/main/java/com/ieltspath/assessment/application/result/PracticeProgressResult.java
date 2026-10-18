package com.ieltspath.assessment.application.result;

import java.util.List;
import java.util.UUID;

public record PracticeProgressResult(List<Key> completed) {
    public record Key(UUID packageId, Integer partNo) {}
}
