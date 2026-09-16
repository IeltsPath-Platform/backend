package com.group01.user.api.dto.response;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phoneNumber,
        String status,
        Set<RoleResponse> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
