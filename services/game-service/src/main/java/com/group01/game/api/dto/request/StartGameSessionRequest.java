package com.group01.game.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record StartGameSessionRequest(
        @NotBlank @Size(max = 50) String gameType,
        @NotBlank @Size(max = 30) String learningDomain,
        @NotBlank @Size(max = 30) String mode,
        UUID topicId,
        @NotEmpty @Size(max = 20) List<UUID> contentIds
) {}
