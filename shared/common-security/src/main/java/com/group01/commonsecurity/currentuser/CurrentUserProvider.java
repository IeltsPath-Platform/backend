package com.group01.commonsecurity.currentuser;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CurrentUserProvider {
    Optional<CurrentUser> currentUser();

    CurrentUser requireCurrentUser();

    default UUID requireUserId() {
        return requireCurrentUser().id();
    }

    default Set<String> requireRoles() {
        return requireCurrentUser().roles();
    }
}
