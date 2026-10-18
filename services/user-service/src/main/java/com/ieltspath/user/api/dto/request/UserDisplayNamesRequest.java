package com.ieltspath.user.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UserDisplayNamesRequest(@NotEmpty @Size(max = 50) List<@NotNull UUID> ids) {}
