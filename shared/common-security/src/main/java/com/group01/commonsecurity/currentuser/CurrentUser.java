package com.group01.commonsecurity.currentuser;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record CurrentUser(UUID id, Set<String> roles) {
    public CurrentUser {
        roles = roles == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(roles));
    }

    public boolean hasRole(String role) {
        return role != null && roles.contains(role);
    }

    public boolean hasAnyRole(String... expectedRoles) {
        if (expectedRoles == null) {
            return false;
        }
        for (String role : expectedRoles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

}
