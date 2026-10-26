package com.ieltspath.game.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SubmitRocketAnswerRequest(@NotBlank @Size(max = 100) String requestId, @NotNull UUID optionId) {}
