package com.ieltspath.game.api.dto.request;

import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record CreateWordlyRoomRequest(
        @NotNull WordlyRoomSettings.PlayerMode playerMode,
        @NotNull @Min(3) @Max(6) Integer initialLives,
        @NotNull WordlyRoomSettings.PlayMode playMode,
        @Min(5) Integer wordLimit,
        Boolean timeEnabled,
        @Pattern(regexp = "A1|A2|B1|B2|C1|C2") String cefrLevel,
        UUID topicId
) {}
