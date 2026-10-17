package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record SchedulePackagePublicationRequest(@NotNull Instant publishAt) {}
