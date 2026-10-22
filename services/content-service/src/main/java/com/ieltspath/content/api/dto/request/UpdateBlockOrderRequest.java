package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record UpdateBlockOrderRequest(@NotEmpty List<UUID> blockIds) {}
