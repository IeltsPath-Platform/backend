package com.ieltspath.user.api.dto.response;

import com.ieltspath.user.application.port.SalesStaffReader.SalesAccount;

import java.util.UUID;

public record SalesStaffResponse(UUID id, String displayName, boolean active) {
    public static SalesStaffResponse from(SalesAccount account) {
        return new SalesStaffResponse(account.id(), account.displayName(), account.active());
    }
}
