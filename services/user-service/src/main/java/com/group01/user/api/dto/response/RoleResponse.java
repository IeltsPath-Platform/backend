package com.group01.user.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record RoleResponse(UUID id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
