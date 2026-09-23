package com.group01.game.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateGameRoomRequest(
        @NotBlank @Size(max = 50) String gameType,
        @NotBlank @Size(max = 30) String learningDomain,
        @NotBlank @Size(max = 30) String mode,
        @Min(2) @Max(32) int maxPlayers,
        @NotEmpty @Size(max = 20) List<UUID> contentIds
) {}
