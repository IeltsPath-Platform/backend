package com.ieltspath.user.application.result;

import java.util.UUID;

public record PublicProfileResult(UUID id, String displayName, String avatarUrl, String bio) {}
