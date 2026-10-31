package com.ieltspath.user.application.result;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicProfileResult(UUID id, String displayName, String avatarUrl, String bio,
                                  BigDecimal targetBand) {}
