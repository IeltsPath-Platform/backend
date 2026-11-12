package com.ieltspath.content.api.dto.internal;

import com.ieltspath.content.application.port.PracticeTestKeyReader;

import java.util.List;
import java.util.UUID;

/** The package of each package version and the package and part of each Writing question that were found. */
public record PracticeTestKeysResponse(List<VersionKey> versions, List<QuestionKey> questions) {
    public record VersionKey(UUID packageVersionId, UUID packageId) {}

    public record QuestionKey(UUID questionVersionId, UUID packageId, Integer partNo) {}

    public static PracticeTestKeysResponse from(PracticeTestKeyReader.Keys keys) {
        return new PracticeTestKeysResponse(
                keys.packageByVersion().entrySet().stream().map(entry -> new VersionKey(entry.getKey(), entry.getValue())).toList(),
                keys.byQuestion().entrySet().stream()
                        .map(entry -> new QuestionKey(entry.getKey(), entry.getValue().packageId(), entry.getValue().partNo())).toList());
    }
}
