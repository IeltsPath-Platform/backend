package com.ieltspath.user.application.port;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface UserDisplayNameReader {
    /** Returns names only for existing users; profile display names take precedence over account full names. */
    Map<UUID, String> findDisplayNamesByIds(Collection<UUID> userIds);
}
