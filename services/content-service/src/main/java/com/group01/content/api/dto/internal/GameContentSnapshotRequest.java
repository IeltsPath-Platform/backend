package com.group01.content.api.dto.internal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record GameContentSnapshotRequest(
        @NotEmpty @Size(max = 50) String gameType,
        @NotEmpty @Size(max = 20) List<UUID> contentIds,
        @NotEmpty String learningDomain
) {
}
