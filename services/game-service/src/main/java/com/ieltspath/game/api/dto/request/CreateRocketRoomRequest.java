package com.ieltspath.game.api.dto.request;

import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Solo rooms only choose the question count (3-20); lives and seconds per question use the defaults and are ignored.
 * Multiplayer hosts choose 5-20 questions, 3-6 lives and 10, 15 or 20 seconds; omitted values use the defaults.
 */
public record CreateRocketRoomRequest(
        @NotNull WordlyRoomSettings.PlayerMode playerMode,
        @NotNull @Min(3) @Max(20) Integer questionCount,
        @Min(3) @Max(6) Integer initialLives,
        Integer secondsPerQuestion
) {}
