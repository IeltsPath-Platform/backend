package com.ieltspath.access.application.port;

import java.util.List;
import java.util.UUID;

/** The sales staff accounts, kept by the user service; this service only reads them. */
public interface SaleDirectory {

    /** Every account with the sales role, active or not. Throws {@code DownstreamUnavailableException} when the user service cannot answer. */
    List<SaleAccount> sales();

    record SaleAccount(UUID id, String displayName, boolean active) {}
}
