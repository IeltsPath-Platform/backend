package com.ieltspath.library.application.result;

import java.util.UUID;

public record CommonMistakeResult(UUID id, UUID senseId, String wrongUsage, String correctUsage, String explanation,
                                  int sortOrder) {
}
