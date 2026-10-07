package com.ieltspath.content.application.result;

import java.util.UUID;

/** A published final-test code of a topic and its current published version. */
public record TopicTestPackageResult(UUID packageId, UUID packageVersionId, String code) {}
