package com.ieltspath.content.api.dto.internal;

import java.util.Map;
import java.util.UUID;

/** Eligible practice sets left per knowledge point, zero included. */
public record PracticeSetAvailabilityResponse(Map<UUID, Integer> counts) {}
