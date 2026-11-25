package com.ieltspath.user.application.port;

import java.util.List;
import java.util.UUID;

/** Reads the accounts that hold the SALES_STAFF role, for the access service that hands keys to them. */
public interface SalesStaffReader {

    /** Every account with the role, newest name order; {@code active} is false for a locked or disabled account. */
    List<SalesAccount> findAll();

    record SalesAccount(UUID id, String displayName, boolean active) {}
}
