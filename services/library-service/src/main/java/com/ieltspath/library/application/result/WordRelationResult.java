package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.vo.RelationKind;

import java.util.UUID;

public record WordRelationResult(UUID id, UUID senseId, String relatedLemma, UUID relatedItemId, RelationKind kind,
                                 int sortOrder) {
}
