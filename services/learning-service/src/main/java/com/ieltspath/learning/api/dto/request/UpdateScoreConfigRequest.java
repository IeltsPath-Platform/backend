package com.ieltspath.learning.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Every value is a fraction of 1 and the request replaces the whole configuration. */
public record UpdateScoreConfigRequest(@NotNull BigDecimal processWeight, @NotNull BigDecimal midWeight,
                                       @NotNull BigDecimal finalWeight, @NotNull BigDecimal practiceShare,
                                       @NotNull BigDecimal practiceTestShare, @NotNull BigDecimal passThreshold,
                                       @NotNull BigDecimal midRemediationBelow) {}
