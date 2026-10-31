package com.ieltspath.game.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/**
 * Wordly rooms use wordLimit and timeEnabled; Rocket rooms use questionCount and secondsPerQuestion (null keeps them).
 * Both use cefrLevel and topicId, where null means any: unlike the other fields, leaving them out clears the choice.
 */
public record UpdateRoomSettingsRequest(
        @NotNull @Min(3) @Max(6) Integer initialLives,
        @Min(5) Integer wordLimit,
        Boolean timeEnabled,
        @Min(3) @Max(20) Integer questionCount,
        Integer secondsPerQuestion,
        @Pattern(regexp = "A1|A2|B1|B2|C1|C2") String cefrLevel,
        UUID topicId
) {}
