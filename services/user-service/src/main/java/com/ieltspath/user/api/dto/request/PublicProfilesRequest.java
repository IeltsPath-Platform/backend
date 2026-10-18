package com.ieltspath.user.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record PublicProfilesRequest(@NotNull @Size(max = 100) List<@NotNull UUID> userIds) {}
