package com.ieltspath.content.api.dto.internal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Either list may be left out; at most 500 ids each, the same limit as the catalog's key filter. */
public record PracticeTestKeysRequest(
        @Size(max = 500, message = "packageVersionIds must hold at most 500 ids")
        List<@NotNull UUID> packageVersionIds,
        @Size(max = 500, message = "questionVersionIds must hold at most 500 ids")
        List<@NotNull UUID> questionVersionIds
) {}
