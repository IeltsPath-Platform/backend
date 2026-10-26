package com.ieltspath.assessment.application.port;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Names of users, from the User Service. A name is only a label: callers must work without it. */
public interface UserDirectory {

    /** Display names by user id; users without a name, and every user when the User Service fails, are absent. */
    Map<UUID, String> displayNames(Collection<UUID> userIds);
}
