package com.ieltspath.library.application.result;

import java.util.UUID;

public record CollocationResult(UUID id, UUID senseId, String phrase, String meaningVi, String example, int sortOrder) {
}
