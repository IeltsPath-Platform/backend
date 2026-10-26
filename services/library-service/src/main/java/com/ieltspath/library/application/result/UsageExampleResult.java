package com.ieltspath.library.application.result;

import java.util.UUID;

public record UsageExampleResult(UUID id, UUID senseId, String sentence, String translationVi, int sortOrder) {
}
