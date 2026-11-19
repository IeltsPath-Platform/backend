package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.Size;

public record RejectAiDraftRequest(@Size(max = 1000, message = "Ghi chú không được dài quá 1000 ký tự") String note) {
}
