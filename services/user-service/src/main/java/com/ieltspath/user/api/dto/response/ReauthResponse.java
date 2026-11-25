package com.ieltspath.user.api.dto.response;

public record ReauthResponse(String reauthToken, int expiresInSeconds, String method) {
}
