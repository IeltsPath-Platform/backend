package com.ieltspath.content.api.dto.internal;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record KnowledgePointPrerequisitesRequest(@NotNull List<@NotNull UUID> ids) {}
