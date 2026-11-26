package com.ieltspath.user.api.dto.request;

public record DeleteAccountRequest(String currentPassword, String googleIdToken) {
}
