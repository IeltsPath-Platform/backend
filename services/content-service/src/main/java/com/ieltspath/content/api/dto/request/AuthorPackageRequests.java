package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class AuthorPackageRequests {

    private AuthorPackageRequests() {
    }

    /** {@code rulesJson} defaults to {@code "{}"} when omitted; a non-JSON value is refused by the use case. */
    public record CreateDraftVersion(@Size(max = 20000) String rulesJson) {
    }

    public record ReorderSections(@NotNull List<UUID> sectionIds) {
    }
}
