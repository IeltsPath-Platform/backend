package com.ieltspath.game.api.dto.request;
import jakarta.validation.constraints.NotNull;
public record ReadyStatusRequest(@NotNull Boolean ready) {}
