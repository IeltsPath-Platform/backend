package com.ieltspath.content.domain.vo;

import java.util.UUID;

/** A configured milestone and the package's current published version, if any. */
public record PracticeTestMilestone(UUID packageId, UUID packageVersionId, String code, String title,
                                    int afterTopicSequence) {}
