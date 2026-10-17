package com.ieltspath.user.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record StudyHeartbeatRequest(@Min(1) long epoch,@Min(1) long sequence,
        @NotNull @Size(max=16) List<@NotNull @Valid Interval> intervals) {
    public record Interval(@Min(0) long startOffsetMs,@Min(1) long endOffsetMs) {}
}
