package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.Size;

public record RejectAiDraftRequest(@Size(max = 1000, message = "note must not exceed 1000 characters") String note) {
}
