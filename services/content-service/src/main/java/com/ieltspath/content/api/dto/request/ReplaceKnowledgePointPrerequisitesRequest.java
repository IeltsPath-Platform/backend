package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ReplaceKnowledgePointPrerequisitesRequest(
        @NotNull @Size(max = 300) List<@NotNull UUID> prerequisiteIds) {}
