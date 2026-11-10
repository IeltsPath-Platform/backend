package com.ieltspath.user.application.result;

import java.util.UUID;

public record GoogleSignInResult(UUID userId, AuthTokenResult tokens) {
}
