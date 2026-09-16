package com.group01.user.api.dto.response;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        String id,
        UUID userId,
        String email,
        List<String> roles
) {
}
