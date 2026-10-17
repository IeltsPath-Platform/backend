package com.ieltspath.content.application.result;

import java.time.Instant;
import java.util.UUID;

public record ScheduledPackagePublicationResult(UUID packageId, UUID versionId, Instant publishAt,
                                                String lastPublishError) {}
